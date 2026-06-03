package br.com.fiap.siase.domain.model;

import br.com.fiap.siase.domain.enums.StatusPedidoCompra;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoCompra {
    private UUID id;
    private Peca peca;
    private Integer quantidadeSolicitada;
    @Builder.Default
    private Integer quantidadeRecebida = 0;
    @Builder.Default
    private StatusPedidoCompra status = StatusPedidoCompra.PENDENTE;
    private String observacoes;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

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
