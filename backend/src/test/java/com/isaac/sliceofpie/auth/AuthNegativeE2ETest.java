package com.isaac.sliceofpie.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class AuthNegativeE2ETest {

    private final RestTemplate rest = new RestTemplate();
    private final String baseUrl = "http://localhost:8080";

    @Test
    void wrong_password_fails_login() {

        String username = AuthTestUtils.uniqueUsername();

        rest.postForEntity(
            baseUrl + "/auth/register",
            Map.of("username", username, "password", "1234"),
            String.class
        );

        try {
            rest.postForEntity(
                baseUrl + "/auth/login",
                Map.of("username", username, "password", "wrong"),
                String.class
            );
            fail("Expected 401");
        } catch (HttpClientErrorException ex) {
            assertEquals(401, ex.getStatusCode().value());
        }
    }

    @Test
    void missing_token_fails_me() {
        try{
            rest.getForEntity(
                baseUrl + "/me",
                String.class);
            fail("Expected 401");
        } catch (HttpClientErrorException ex){
            assertEquals(401, ex.getStatusCode().value());
        }
    }

    @Test
    void invalid_token_fails_me() {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid.token.here");

        HttpEntity<Void> req = new HttpEntity<>(headers);

        try{
            rest.exchange(
                baseUrl + "/me",
                HttpMethod.GET,
                req,
                String.class);
            fail("Expected 401");
        } catch(HttpClientErrorException ex){
            assertEquals(401, ex.getStatusCode().value());
        }
    }
}