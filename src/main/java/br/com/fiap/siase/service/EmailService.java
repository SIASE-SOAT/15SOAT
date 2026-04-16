package br.com.fiap.siase.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    public void enviarConfirmacaoAgendamento(String emailCliente, String nomeCliente,
                                             String dataHora, String veiculo) {
        log.info("""
                [EMAIL] Para: {} <{}>
                Assunto: Agendamento confirmado - SIASE Oficina
                Corpo:
                  Olá, {}! Seu agendamento foi registrado com sucesso.
                  Veículo: {}
                  Data/Hora: {}
                  Aguardamos você na oficina!
                """, nomeCliente, emailCliente, nomeCliente, veiculo, dataHora);
    }

    public void enviarOrcamentoParaAprovacao(String emailCliente, String nomeCliente,
                                             String osNumero, String total) {
        log.info("""
                [EMAIL] Para: {} <{}>
                Assunto: Orçamento disponível para aprovação - OS {}
                Corpo:
                  Olá, {}! O diagnóstico do seu veículo foi concluído.
                  OS: {}
                  Total do orçamento: R$ {}
                  Por favor, entre em contato para aprovar ou recusar o serviço.
                """, nomeCliente, emailCliente, osNumero, nomeCliente, osNumero, total);
    }

    public void enviarOrcamentoAprovado(String emailCliente, String nomeCliente, String osNumero) {
        log.info("""
                [EMAIL] Para: {} <{}>
                Assunto: Orçamento aprovado - OS {} em execução
                Corpo:
                  Olá, {}! Seu orçamento foi aprovado e o serviço está em execução.
                  OS: {}
                  Acompanhe o status em tempo real pelo número da OS.
                """, nomeCliente, emailCliente, osNumero, nomeCliente, osNumero);
    }

    public void enviarOrcamentoCancelado(String emailCliente, String nomeCliente, String osNumero) {
        log.info("""
                [EMAIL] Para: {} <{}>
                Assunto: Ordem de Serviço cancelada - OS {}
                Corpo:
                  Olá, {}! Sua Ordem de Serviço {} foi cancelada conforme solicitado.
                  Caso mude de ideia, entre em contato com a oficina.
                """, nomeCliente, emailCliente, osNumero, nomeCliente, osNumero);
    }

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
}
