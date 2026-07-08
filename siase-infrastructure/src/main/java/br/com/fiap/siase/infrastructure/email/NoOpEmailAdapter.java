package br.com.fiap.siase.infrastructure.email;

import br.com.fiap.siase.domain.port.EmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnExpression("'${spring.mail.host:}' == ''")
public class NoOpEmailAdapter implements EmailPort {

    @Override
    public void enviarOrcamentoParaAprovacao(String emailCliente, String nomeCliente,
                                             String osNumero, String total) {
        log.info("[EMAIL-NOOP] Orçamento para aprovação | Para: {} | OS: {} | Total: {}", emailCliente, osNumero, total);
    }

    @Override
    public void enviarOrcamentoAprovado(String emailCliente, String nomeCliente, String osNumero) {
        log.info("[EMAIL-NOOP] Orçamento aprovado | Para: {} | OS: {}", emailCliente, osNumero);
    }

    @Override
    public void enviarOrcamentoCancelado(String emailCliente, String nomeCliente, String osNumero) {
        log.info("[EMAIL-NOOP] Orçamento cancelado | Para: {} | OS: {}", emailCliente, osNumero);
    }

    @Override
    public void enviarConfirmacaoPagamento(String emailCliente, String nomeCliente,
                                           String osNumero, String valor, String formaPagamento) {
        log.info("[EMAIL-NOOP] Pagamento confirmado | Para: {} | OS: {} | Valor: {}", emailCliente, osNumero, valor);
    }
}
