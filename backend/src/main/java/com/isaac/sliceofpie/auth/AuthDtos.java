package com.isaac.sliceofpie.auth;

public class AuthDtos {

    public record RegisterRequest(String username, String password) {}

    public record RegisterResponse(String username) {}

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String token) {}
}