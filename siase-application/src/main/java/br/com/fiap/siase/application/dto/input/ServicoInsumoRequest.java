package br.com.fiap.siase.application.dto.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ServicoInsumoRequest(
        @NotNull(message = "ID da peça é obrigatório")
        UUID pecaId,
        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser maior que zero")
        Integer quantidade
) {}
