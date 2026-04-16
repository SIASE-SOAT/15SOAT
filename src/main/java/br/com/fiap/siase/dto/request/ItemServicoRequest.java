package br.com.fiap.siase.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ItemServicoRequest(
        @NotNull(message = "Id do serviço é obrigatório")
        UUID servicoId,
        String observacoes
) {}
