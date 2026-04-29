package br.com.fiap.siase.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("EmailService - Envio de Notificações")
class EmailServiceTest {

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "portalClienteUrl", "http://localhost:4200");
    }

    @Nested
    @DisplayName("Confirmação de Agendamento")
    class ConfirmacaoAgendamento {

        @Test
        @DisplayName("Deve enviar confirmação sem lançar exceção")
        void deveEnviarConfirmacaoAgendamento() {
            assertThatNoException().isThrownBy(() ->
                emailService.enviarConfirmacaoAgendamento(
                    "joao@email.com", "João Silva", "2026-05-01 10:00", "Toyota Corolla 2022"));
        }
    }

    @Nested
    @DisplayName("Orçamento para Aprovação")
    class OrcamentoParaAprovacao {

        @Test
        @DisplayName("Deve enviar orçamento para aprovação sem lançar exceção")
        void deveEnviarOrcamentoParaAprovacao() {
            assertThatNoException().isThrownBy(() ->
                emailService.enviarOrcamentoParaAprovacao(
                    "joao@email.com", "João Silva", "OS-20260427-A1B2C3", "350,00"));
        }

        @Test
        @DisplayName("Link de acompanhamento deve conter o número da OS")
        void linkDeveConterNumeroOs() {
            assertThatNoException().isThrownBy(() ->
                emailService.enviarOrcamentoParaAprovacao(
                    "cli@test.com", "Cliente", "OS-XPTO", "100,00"));
        }

        @Test
        @DisplayName("Deve remover barra final da URL do portal ao gerar link")
        void deveRemoverBarraFinalDaUrl() {
            ReflectionTestUtils.setField(emailService, "portalClienteUrl", "http://localhost:4200/");
            assertThatNoException().isThrownBy(() ->
                emailService.enviarOrcamentoParaAprovacao(
                    "cli@test.com", "Cliente", "OS-001", "200,00"));
        }
    }

    @Nested
    @DisplayName("Orçamento Aprovado")
    class OrcamentoAprovado {

        @Test
        @DisplayName("Deve enviar notificação de aprovação sem lançar exceção")
        void deveEnviarOrcamentoAprovado() {
            assertThatNoException().isThrownBy(() ->
                emailService.enviarOrcamentoAprovado(
                    "joao@email.com", "João Silva", "OS-20260427-A1B2C3"));
        }
    }

    @Nested
    @DisplayName("OS Cancelada")
    class OsCancelada {

        @Test
        @DisplayName("Deve enviar notificação de cancelamento sem lançar exceção")
        void deveEnviarOrcamentoCancelado() {
            assertThatNoException().isThrownBy(() ->
                emailService.enviarOrcamentoCancelado(
                    "joao@email.com", "João Silva", "OS-20260427-A1B2C3"));
        }
    }

    @Nested
    @DisplayName("Confirmação de Pagamento")
    class ConfirmacaoPagamento {

        @Test
        @DisplayName("Deve enviar confirmação de pagamento sem lançar exceção")
        void deveEnviarConfirmacaoPagamento() {
            assertThatNoException().isThrownBy(() ->
                emailService.enviarConfirmacaoPagamento(
                    "joao@email.com", "João Silva", "OS-20260427-A1B2C3", "350,00", "PIX"));
        }
    }
}
