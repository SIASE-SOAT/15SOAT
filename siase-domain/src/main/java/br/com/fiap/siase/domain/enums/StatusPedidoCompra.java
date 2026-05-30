package br.com.fiap.siase.domain.enums;

public enum StatusPedidoCompra {

    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    RECEBIDO("Recebido"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedidoCompra(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
