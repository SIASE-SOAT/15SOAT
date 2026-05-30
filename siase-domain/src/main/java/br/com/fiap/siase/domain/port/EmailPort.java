package br.com.fiap.siase.domain.port;

public interface EmailPort {

    void enviarOrcamentoParaAprovacao(String emailCliente, String nomeCliente, String osNumero, String total);

    void enviarOrcamentoAprovado(String emailCliente, String nomeCliente, String osNumero);

    void enviarOrcamentoCancelado(String emailCliente, String nomeCliente, String osNumero);

    void enviarConfirmacaoPagamento(String emailCliente, String nomeCliente, String osNumero, String valor, String formaPagamento);
}
