package br.com.fiap.siase.dto.response;

public record LoginResponse(String token, long expiresIn) {}
