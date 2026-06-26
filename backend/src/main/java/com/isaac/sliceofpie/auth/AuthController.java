package com.isaac.sliceofpie.auth;

import com.isaac.sliceofpie.auth.AuthDtos.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        System.out.println("REGISTER HIT: " + req.username());

        authService.register(req.username(), req.password());
        return ResponseEntity.status(201).build();
    }
}