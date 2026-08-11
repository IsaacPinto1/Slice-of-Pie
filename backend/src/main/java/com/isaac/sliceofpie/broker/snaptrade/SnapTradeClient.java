package com.isaac.sliceofpie.broker.snaptrade;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

@Component
public class SnapTradeClient {

    private static final String BASE_URL = "https://api.snaptrade.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String consumerKey;

    public SnapTradeClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${snaptrade.client-id}") String clientId,
            @Value("${snaptrade.consumer-key}") String consumerKey
    ) {
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .build();
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.consumerKey = consumerKey;
    }

    public <T> T get(String path, Class<T> responseType) {
        return execute("GET", path, null, responseType);
    }

    public <T, B> T post(String path, B body, Class<T> responseType) {
        return execute("POST", path, body, responseType);
    }

    private <T> T execute(
            String method,
            String path,
            Object body,
            Class<T> responseType
    ) {
        long timestamp = Instant.now().getEpochSecond();

        String queryString =
                "clientId=" + clientId +
                "&timestamp=" + timestamp;

        String signature = generateSignature(
                path,
                queryString,
                body
        );

        try {
            RestClient.RequestHeadersSpec<?> request;

            if ("POST".equals(method)) {
                request = restClient.post()
                        .uri(path + "?" + queryString)
                        .header("Signature", signature)
                        .body(body);
            } else {
                request = restClient.get()
                        .uri(path + "?" + queryString)
                        .header("Signature", signature);
            }

            return request
                    .retrieve()
                    .body(responseType);

        } catch (RestClientException e) {
            throw new SnapTradeException(
                    "SnapTrade " + method + " request failed: " + path,
                    e
            );
        }
    }

    private String generateSignature(
            String path,
            String queryString,
            Object body
    ) {
        try {
            Map<String, Object> payload = new TreeMap<>();

            payload.put("content", body);
            payload.put("path", path);
            payload.put("query", queryString);

            String data = objectMapper.writeValueAsString(payload);

            SecretKeySpec key = new SecretKeySpec(
                    consumerKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);

            byte[] hash = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hash);

        } catch (JsonProcessingException |
                 NoSuchAlgorithmException |
                 InvalidKeyException e) {
            throw new SnapTradeSigningException(
                    "Failed to generate SnapTrade signature",
                    e
            );
        }
    }
}