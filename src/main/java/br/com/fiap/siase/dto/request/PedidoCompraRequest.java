package br.com.fiap.siase.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PedidoCompraRequest(
        @NotNull(message = "Peça é obrigatória")
        UUID pecaId,
        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
        Integer quantidadeSolicitada,
        String observacoes
) {}
