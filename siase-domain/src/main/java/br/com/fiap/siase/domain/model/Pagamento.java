package br.com.fiap.siase.domain.model;

import br.com.fiap.siase.domain.enums.FormaPagamento;
import br.com.fiap.siase.domain.enums.StatusPagamento;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pagamentos")
public class Pagamento extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_de_servico_id", nullable = false, unique = true)
    private OrdemDeServico ordemDeServico;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 30)
    private FormaPagamento formaPagamento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;

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
