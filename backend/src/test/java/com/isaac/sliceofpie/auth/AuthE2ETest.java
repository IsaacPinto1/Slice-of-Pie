package com.isaac.sliceofpie.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import com.isaac.sliceofpie.auth.AuthDtos.LoginResponse;
import com.isaac.sliceofpie.auth.AuthDtos.RegisterResponse;
import com.isaac.sliceofpie.users.UserDtos.MeResponse;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class AuthE2ETest {

    private final RestTemplate rest = new RestTemplate();
    private final String baseUrl = "http://localhost:8080";

    @Test
    void full_auth_flow() {

        String username = AuthTestUtils.uniqueUsername();

        // REGISTER
        ResponseEntity<RegisterResponse> registerRes = rest.postForEntity(
                baseUrl + "/auth/register",
                Map.of("username", username, "password", "1234"),
                RegisterResponse.class
        );

        assertEquals(201, registerRes.getStatusCode().value());

        // LOGIN
        ResponseEntity<LoginResponse> loginRes = rest.postForEntity(
                baseUrl + "/auth/login",
                Map.of("username", username, "password", "1234"),
                LoginResponse.class
        );

        assertEquals(200, loginRes.getStatusCode().value());
        assertNotNull(loginRes.getBody());

        String token = (String) loginRes.getBody().token();
        assertNotNull(token);

        // /me
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<MeResponse> meRes = rest.exchange(
                baseUrl + "/me",
                HttpMethod.GET,
                request,
                MeResponse.class
        );

        assertEquals(200, meRes.getStatusCode().value());
        assertEquals(username, meRes.getBody().username());
    }
}