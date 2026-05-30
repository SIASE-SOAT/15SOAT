package br.com.fiap.siase.application.dto.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MovimentacaoEstoqueRequest(
        @NotNull(message = "Operação é obrigatória")
        Operacao operacao,
        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser maior que zero")
        Integer quantidade
) {
    public enum Operacao { ENTRADA, SAIDA }
}
