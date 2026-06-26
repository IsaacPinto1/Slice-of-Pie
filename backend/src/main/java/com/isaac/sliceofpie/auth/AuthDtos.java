package com.isaac.sliceofpie.auth;

public class AuthDtos {

    public record RegisterRequest(String username, String password) {}

    public record LoginRequest(String username, String password) {}

    public record AuthResponse(String token) {}
}