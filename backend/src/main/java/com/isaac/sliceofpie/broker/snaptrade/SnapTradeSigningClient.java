package com.isaac.sliceofpie.broker.snaptrade;

import com.isaac.sliceofpie.broker.exception.BrokerLookupException;
import com.isaac.sliceofpie.broker.exception.RequestSigningException;

import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Handles SnapTrade's HMAC request-signing scheme and issues signed GET
 * requests against it. Only SnapTradeAccountClient should call this - it
 * stays SnapTrade-specific by design so nothing else in the broker package
 * ever needs to know how SnapTrade signs a request. Failures are surfaced
 * as the generic BrokerLookupException / RequestSigningException from
 * broker.exception, never a SnapTrade-named type, so callers outside this
 * package don't leak the provider's identity either.
 */
@Component
public class SnapTradeSigningClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String consumerKey;

    public SnapTradeSigningClient(
            @Qualifier("snapTradeRestClientBuilder") RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${snaptrade.client-id:}") String clientId,
            @Value("${snaptrade.consumer-key:}") String consumerKey
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.consumerKey = consumerKey;
    }

    public <T> T get(
            String path,
            Map<String, String> queryParameters,
            Class<T> responseType
    ) {
        long timestamp = Instant.now().getEpochSecond();

        String queryString = buildQueryString(queryParameters, timestamp);

        String signature = generateSignature(
                path,
                queryString,
                null
        );

        try {
            return restClient.get()
                    .uri(path + "?" + queryString)
                    .header("Signature", signature)
                    .retrieve()
                    .body(responseType);

        } catch (RestClientException e) {
            throw new BrokerLookupException(
                    "SnapTrade GET request failed: " + path,
                    e
            );
        }
    }

    private String buildQueryString(
            Map<String, String> queryParameters,
            long timestamp
    ) {
        String additionalParameters = queryParameters.entrySet()
                .stream()
                .map(entry ->
                        entry.getKey() + "=" + entry.getValue()
                )
                .collect(Collectors.joining("&"));

        String authenticationParameters =
                "clientId=" + clientId +
                "&timestamp=" + timestamp;

        if (additionalParameters.isEmpty()) {
            return authenticationParameters;
        }

        return authenticationParameters + "&" + additionalParameters;
    }

    private String generateSignature(
            String path,
            String queryString,
            Object body
    ) {
        try {
            /*
             * SnapTrade requires the signature payload to have
             * alphabetically sorted keys.
             */
            Map<String, Object> payload = new TreeMap<>();
            payload.put("content", body);
            payload.put("path", path);
            payload.put("query", queryString);

            String data = objectMapper.writeValueAsString(payload);

            SecretKeySpec secretKey = new SecretKeySpec(
                    consumerKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hash);

        } catch (JsonParseException |
                 NoSuchAlgorithmException |
                 InvalidKeyException e) {
            throw new RequestSigningException(
                    "Failed to generate SnapTrade request signature",
                    e
            );
        }
    }
}
