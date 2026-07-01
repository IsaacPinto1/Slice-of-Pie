package com.isaac.sliceofpie.auth;

import com.isaac.sliceofpie.auth.AuthDtos.RegisterRequest;
import com.isaac.sliceofpie.auth.AuthDtos.RegisterResponse;
import com.isaac.sliceofpie.auth.AuthDtos.LoginRequest;

import org.springframework.http.HttpStatus;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(req.username()));
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest req) {
        authService.login(
                req.username(),
                req.password());
        return ResponseEntity.ok().build();
    }
}