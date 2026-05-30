package br.com.fiap.siase.application.dto.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ItemPecaRequest(
        @NotNull(message = "Id da peça é obrigatório")
        UUID pecaId,
        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
        Integer quantidade
) {}
