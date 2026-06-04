package br.com.fiap.siase.domain.model;

import br.com.fiap.siase.domain.enums.FormaPagamento;
import br.com.fiap.siase.domain.enums.StatusPagamento;
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
public class Pagamento {
    private UUID id;
    private OrdemDeServico ordemDeServico;
    private FormaPagamento formaPagamento;
    private BigDecimal valor;
    @Builder.Default
    private StatusPagamento status = StatusPagamento.PENDENTE;
    private LocalDateTime dataPagamento;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public void confirmar() {
        if (this.status != StatusPagamento.PENDENTE) {
            throw new IllegalStateException("Apenas pagamentos pendentes podem ser confirmados.");
        }
        this.status = StatusPagamento.PAGO;
        this.dataPagamento = LocalDateTime.now();
    }

    public void cancelar() {
        if (this.status == StatusPagamento.PAGO) {
            throw new IllegalStateException("Pagamento já confirmado não pode ser cancelado.");
        }
        this.status = StatusPagamento.CANCELADO;
    }
}
