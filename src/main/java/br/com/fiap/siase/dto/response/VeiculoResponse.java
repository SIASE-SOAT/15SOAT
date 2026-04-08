package br.com.fiap.siase.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record VeiculoResponse(
        UUID id,
        String placa,
        String marca,
        String modelo,
        Integer ano,
        String cor,
        Boolean ativo,
        UUID clienteId,
        String clienteNome,
        LocalDateTime criadoEm
) {}