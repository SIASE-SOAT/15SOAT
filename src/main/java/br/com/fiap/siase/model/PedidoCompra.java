package br.com.fiap.siase.model;

import br.com.fiap.siase.model.enums.StatusPedidoCompra;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pedidos_compra")
public class PedidoCompra extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peca_id", nullable = false)
    private Peca peca;

    @Column(name = "quantidade_solicitada", nullable = false)
    private Integer quantidadeSolicitada;

    @Column(name = "quantidade_recebida", nullable = false)
    private Integer quantidadeRecebida = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedidoCompra status = StatusPedidoCompra.PENDENTE;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    public void aprovar() {
        if (this.status != StatusPedidoCompra.PENDENTE) {
            throw new IllegalStateException("Apenas pedidos PENDENTES podem ser aprovados.");
        }
        this.status = StatusPedidoCompra.APROVADO;
    }

    public void receber(int quantidadeRecebida) {
        if (this.status != StatusPedidoCompra.APROVADO) {
            throw new IllegalStateException("Apenas pedidos APROVADOS podem ser recebidos.");
        }
        this.quantidadeRecebida = quantidadeRecebida;
        this.status = StatusPedidoCompra.RECEBIDO;
        this.peca.devolverEstoque(quantidadeRecebida);
    }

    public void cancelar() {
        if (this.status == StatusPedidoCompra.RECEBIDO) {
            throw new IllegalStateException("Pedido já recebido não pode ser cancelado.");
        }
        this.status = StatusPedidoCompra.CANCELADO;
    }
}
