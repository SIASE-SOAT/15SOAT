package br.com.fiap.siase.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PecaRequest(

        @NotBlank(message = "Código é obrigatório")
        String codigo,

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        String descricao,

        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal preco,

        @Min(value = 0, message = "Quantidade em estoque não pode ser negativa")
        Integer quantidadeEstoque,

        @Min(value = 0, message = "Estoque mínimo não pode ser negativo")
        Integer estoqueMinimo,

        String unidadeMedida
) {}
