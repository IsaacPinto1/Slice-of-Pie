package com.isaac.sliceofpie.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

/**
 * Mirrors FinnhubClientConfig's shape for the SnapTrade-backed broker
 * client: a shared, base-URL-preconfigured RestClient.Builder,
 * prototype-scoped so each injection point gets its own builder instance
 * rather than sharing (and potentially mutating) one singleton.
 *
 * Base URL is the bare domain, not .../api/v1 - SnapTrade's HMAC signature
 * is computed over the full request path (e.g. "/api/v1/accounts"), so
 * SnapTradeSigningClient needs the whole path itself to sign, rather than
 * have part of it hidden inside a preconfigured base URL.
 */
@Configuration
public class SnapTradeClientConfig {

    public static final String SNAPTRADE_BASE_URL = "https://api.snaptrade.com";

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RestClient.Builder snapTradeRestClientBuilder() {
        return RestClient.builder().baseUrl(SNAPTRADE_BASE_URL);
    }
}
