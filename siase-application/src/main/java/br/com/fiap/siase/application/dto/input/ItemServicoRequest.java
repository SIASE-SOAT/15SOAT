package br.com.fiap.siase.application.dto.input;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ItemServicoRequest(
        @NotNull(message = "Id do serviço é obrigatório")
        UUID servicoId,
        String observacoes
) {}
