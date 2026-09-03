package br.com.fiap.siase.application.dto.input;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ServicoRequest(
        @NotBlank(message = "Nome é obrigatório")
        String nome,
        String descricao,
        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal preco,
        @Min(value = 1, message = "Tempo estimado deve ser maior que zero")
        Integer tempoEstimadoMinutos
) {}
