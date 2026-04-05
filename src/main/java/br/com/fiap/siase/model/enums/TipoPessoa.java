package br.com.fiap.siase.model.enums;

public enum TipoPessoa {

    PF("Pessoa Física"),
    PJ("Pessoa Jurídica");

    private final String descricao;

    TipoPessoa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
