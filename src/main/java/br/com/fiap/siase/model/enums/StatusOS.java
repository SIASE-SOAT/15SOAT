package br.com.fiap.siase.model.enums;

public enum StatusOS {

    RECEBIDA("Recebida"),
    EM_DIAGNOSTICO("Em Diagnóstico"),
    AGUARDANDO_APROVACAO("Aguardando Aprovação"),
    EM_EXECUCAO("Em Execução"),
    FINALIZADA("Finalizada"),
    ENTREGUE("Entregue"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusOS(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
