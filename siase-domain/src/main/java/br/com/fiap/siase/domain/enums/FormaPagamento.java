package br.com.fiap.siase.domain.enums;

public enum FormaPagamento {

    DINHEIRO("Dinheiro"),
    CARTAO_DEBITO("Cartão de Débito"),
    CARTAO_CREDITO("Cartão de Crédito"),
    PIX("Pix"),
    TRANSFERENCIA("Transferência Bancária");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
