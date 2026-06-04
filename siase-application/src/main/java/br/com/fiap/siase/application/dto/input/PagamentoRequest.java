package br.com.fiap.siase.application.dto.input;

import br.com.fiap.siase.domain.enums.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagamentoRequest(
        @NotNull(message = "Forma de pagamento é obrigatória")
        FormaPagamento formaPagamento,
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal valor
) {}
