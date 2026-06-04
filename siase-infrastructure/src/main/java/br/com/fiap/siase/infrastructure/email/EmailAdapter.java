package br.com.fiap.siase.infrastructure.email;

import br.com.fiap.siase.domain.port.EmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailAdapter implements EmailPort {

    @Value("${cliente.portal-url}")
    private String portalClienteUrl;

    @Override
    public void enviarOrcamentoParaAprovacao(String emailCliente, String nomeCliente,
                                             String osNumero, String total) {
        String linkAcompanhamento = linkAcompanhamento(osNumero);
        log.info("""
                [EMAIL] Para: {} <{}>
                Assunto: Orçamento disponível para aprovação - OS {}
                Corpo:
                  Olá, {}! O diagnóstico do seu veículo foi concluído.
                  OS: {}
                  Total do orçamento: R$ {}
                  Para aprovar ou recusar o orçamento, acesse o link abaixo:
                  {}
                """, nomeCliente, emailCliente, osNumero, nomeCliente, osNumero, total, linkAcompanhamento);
    }

    @Override
    public void enviarOrcamentoAprovado(String emailCliente, String nomeCliente, String osNumero) {
        String linkAcompanhamento = linkAcompanhamento(osNumero);
        log.info("""
                [EMAIL] Para: {} <{}>
                Assunto: Orçamento aprovado - OS {} em execução
                Corpo:
                  Olá, {}! Seu orçamento foi aprovado e o serviço está em execução.
                  OS: {}
                  Acompanhe o status em tempo real em: {}
                """, nomeCliente, emailCliente, osNumero, nomeCliente, osNumero, linkAcompanhamento);
    }

    @Override
    public void enviarOrcamentoCancelado(String emailCliente, String nomeCliente, String osNumero) {
        String linkAcompanhamento = linkAcompanhamento(osNumero);
        log.info("""
                [EMAIL] Para: {} <{}>
                Assunto: Ordem de Serviço cancelada - OS {}
                Corpo:
                  Olá, {}! Sua Ordem de Serviço {} foi cancelada conforme solicitado.
                  Caso mude de ideia, entre em contato com a oficina.
                  Acompanhe detalhes em: {}
                """, nomeCliente, emailCliente, osNumero, nomeCliente, osNumero, linkAcompanhamento);
    }

    @Override
    public void enviarConfirmacaoPagamento(String emailCliente, String nomeCliente,
                                           String osNumero, String valor, String formaPagamento) {
        log.info("""
                [EMAIL] Para: {} <{}>
                Assunto: Pagamento confirmado - OS {}
                Corpo:
                  Olá, {}! Seu pagamento foi confirmado com sucesso.
                  OS: {}
                  Valor pago: R$ {}
                  Forma de pagamento: {}
                  Obrigado pela preferência!
                """, nomeCliente, emailCliente, osNumero, nomeCliente, osNumero, valor, formaPagamento);
    }

    private String linkAcompanhamento(String osNumero) {
        return String.format("%s/acompanhar/%s", portalClienteUrl.replaceAll("/$", ""), osNumero);
    }
}
