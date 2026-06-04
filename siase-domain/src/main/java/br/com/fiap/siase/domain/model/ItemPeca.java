package br.com.fiap.siase.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPeca {
    private UUID id;
    private OrdemDeServico ordemDeServico;
    private Peca peca;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private LocalDateTime criadoEm;

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
