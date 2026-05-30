package br.com.fiap.siase.application.dto.output;

public record LoginResponse(String token, long expiresIn) {}
