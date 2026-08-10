package com.isaac.sliceofpie.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

/**
 * Central place for Finnhub RestClient configuration, shared by every
 * Finnhub-backed lookup client (instrument search, price lookup, ...).
 *
 * Exposes a RestClient.Builder pre-configured with Finnhub's base URL, so
 * individual clients don't each hardcode BASE_URL / RestClient.builder()
 * themselves - they just inject this bean, apply any client-specific
 * customization, and build().
 *
 * Prototype-scoped (matching Spring Boot's own default RestClient.Builder
 * bean) so every injection point gets its own builder instance rather than
 * sharing - and potentially mutating - one singleton across clients.
 */
@Configuration
public class FinnhubClientConfig {

    public static final String FINNHUB_BASE_URL = "https://finnhub.io/api/v1";

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RestClient.Builder finnhubRestClientBuilder() {
        return RestClient.builder().baseUrl(FINNHUB_BASE_URL);
    }
}
