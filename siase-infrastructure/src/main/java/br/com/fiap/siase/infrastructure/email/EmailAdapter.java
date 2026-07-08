package br.com.fiap.siase.infrastructure.email;

import br.com.fiap.siase.domain.port.EmailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnExpression("'${spring.mail.host:}' != ''")
public class EmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;

    @Value("${cliente.portal-url}")
    private String portalClienteUrl;

    @Value("${email.from}")
    private String emailFrom;

    public EmailAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarOrcamentoParaAprovacao(String emailCliente, String nomeCliente,
                                             String osNumero, String total) {
        String link = linkAcompanhamento(osNumero);
        String corpo = """
                <h2>Olá, %s!</h2>
                <p>O diagnóstico do seu veículo foi concluído e o orçamento está disponível para aprovação.</p>
                <table>
                  <tr><td><strong>OS:</strong></td><td>%s</td></tr>
                  <tr><td><strong>Total do orçamento:</strong></td><td>R$ %s</td></tr>
                </table>
                <br/>
                <a href="%s" style="background:#1976d2;color:#fff;padding:10px 20px;text-decoration:none;border-radius:4px;">
                  Aprovar ou recusar orçamento
                </a>
                <p>Ou acesse pelo link: <a href="%s">%s</a></p>
                <br/><br/>
                <small>SIASE — Sistema Integrado de Atendimento e Execução de Serviços</small>
                """.formatted(nomeCliente, osNumero, total, link, link, link);

        enviar(emailCliente, "Orçamento disponível para aprovação - OS " + osNumero, corpo);
    }

    @Override
    public void enviarOrcamentoAprovado(String emailCliente, String nomeCliente, String osNumero) {
        String link = linkAcompanhamento(osNumero);
        String corpo = """
                <h2>Olá, %s!</h2>
                <p>Seu orçamento foi <strong>aprovado</strong> e o serviço está em execução.</p>
                <table>
                  <tr><td><strong>OS:</strong></td><td>%s</td></tr>
                </table>
                <br/>
                <a href="%s" style="background:#388e3c;color:#fff;padding:10px 20px;text-decoration:none;border-radius:4px;">
                  Acompanhar status em tempo real
                </a>
                <p>Ou acesse pelo link: <a href="%s">%s</a></p>
                <br/><br/>
                <small>SIASE — Sistema Integrado de Atendimento e Execução de Serviços</small>
                """.formatted(nomeCliente, osNumero, link, link, link);

        enviar(emailCliente, "Orçamento aprovado - OS " + osNumero + " em execução", corpo);
    }

    @Override
    public void enviarOrcamentoCancelado(String emailCliente, String nomeCliente, String osNumero) {
        String link = linkAcompanhamento(osNumero);
        String corpo = """
                <h2>Olá, %s!</h2>
                <p>Sua Ordem de Serviço foi <strong>cancelada</strong> conforme solicitado.</p>
                <table>
                  <tr><td><strong>OS:</strong></td><td>%s</td></tr>
                </table>
                <p>Caso mude de ideia, entre em contato com a oficina.</p>
                <br/>
                <a href="%s" style="background:#757575;color:#fff;padding:10px 20px;text-decoration:none;border-radius:4px;">
                  Ver detalhes da OS
                </a>
                <p>Ou acesse pelo link: <a href="%s">%s</a></p>
                <br/><br/>
                <small>SIASE — Sistema Integrado de Atendimento e Execução de Serviços</small>
                """.formatted(nomeCliente, osNumero, link, link, link);

        enviar(emailCliente, "Ordem de Serviço cancelada - OS " + osNumero, corpo);
    }

    @Override
    public void enviarConfirmacaoPagamento(String emailCliente, String nomeCliente,
                                           String osNumero, String valor, String formaPagamento) {
        String link = linkAcompanhamento(osNumero);
        String corpo = """
                <h2>Olá, %s!</h2>
                <p>Seu pagamento foi confirmado com sucesso. Obrigado pela preferência!</p>
                <table>
                  <tr><td><strong>OS:</strong></td><td>%s</td></tr>
                  <tr><td><strong>Valor pago:</strong></td><td>R$ %s</td></tr>
                  <tr><td><strong>Forma de pagamento:</strong></td><td>%s</td></tr>
                </table>
                <p>Acompanhe os detalhes da sua OS pelo link: <a href="%s">%s</a></p>
                <br/><br/>
                <small>SIASE — Sistema Integrado de Atendimento e Execução de Serviços</small>
                """.formatted(nomeCliente, osNumero, valor, formaPagamento, link, link);

        enviar(emailCliente, "Pagamento confirmado - OS " + osNumero, corpo);
    }

    private void enviar(String destinatario, String assunto, String corpoHtml) {
        log.info("[EMAIL] Preparando envio | Host: {} | De: {} | Para: {} | Assunto: {}",
                mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl ? impl.getHost() : "?",
                emailFrom, destinatario, assunto);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailFrom);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpoHtml, true);
            mailSender.send(message);
            log.info("[EMAIL] Enviado com sucesso para {} | Assunto: {}", destinatario, assunto);
        } catch (MessagingException e) {
            log.error("[EMAIL] Falha ao montar mensagem para {} | Assunto: {}", destinatario, assunto, e);
        } catch (org.springframework.mail.MailException e) {
            log.error("[EMAIL] Falha ao enviar para {} | Assunto: {}", destinatario, assunto, e);
        }
    }

    private String linkAcompanhamento(String osNumero) {
        return String.format("%s/acompanhar/%s", portalClienteUrl.replaceAll("/$", ""), osNumero);
    }
}
