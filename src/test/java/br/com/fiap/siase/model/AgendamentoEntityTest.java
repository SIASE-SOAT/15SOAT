package br.com.fiap.siase.model;

import br.com.fiap.siase.model.enums.StatusAgendamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agendamento - Regras de Domínio")
class AgendamentoEntityTest {

    @Nested
    @DisplayName("Confirmar")
    class Confirmar {

        @Test
        @DisplayName("Deve confirmar agendamento com status AGENDADO")
        void deveConfirmarAgendamento() {
            Agendamento agendamento = new Agendamento();

            agendamento.confirmar();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        }

        @Test
        @DisplayName("Deve rejeitar confirmação fora do status AGENDADO")
        void deveRejeitarConfirmacaoInvalida() {
            Agendamento agendamento = new Agendamento();
            agendamento.confirmar();

            assertThatThrownBy(agendamento::confirmar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AGENDADO");
        }
    }

    @Nested
    @DisplayName("Cancelar")
    class Cancelar {

        @Test
        @DisplayName("Deve cancelar agendamento ainda não realizado")
        void deveCancelarAgendamento() {
            Agendamento agendamento = new Agendamento();

            agendamento.cancelar();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        }

        @Test
        @DisplayName("Deve rejeitar cancelamento de agendamento realizado")
        void deveRejeitarCancelamentoDeRealizado() {
            Agendamento agendamento = new Agendamento();
            agendamento.setStatus(StatusAgendamento.REALIZADO);

            assertThatThrownBy(agendamento::cancelar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não pode ser cancelado");
        }
    }

    @Nested
    @DisplayName("Realizar")
    class Realizar {

        @Test
        @DisplayName("Deve realizar agendamento confirmado e vincular OS")
        void deveRealizarAgendamentoConfirmado() {
            Agendamento agendamento = new Agendamento();
            OrdemDeServico ordem = new OrdemDeServico();
            agendamento.confirmar();

            agendamento.realizar(ordem);

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.REALIZADO);
            assertThat(agendamento.getOrdemDeServico()).isSameAs(ordem);
        }

        @Test
        @DisplayName("Deve rejeitar realização fora do status CONFIRMADO")
        void deveRejeitarRealizacaoInvalida() {
            Agendamento agendamento = new Agendamento();

            var ordem = new OrdemDeServico();
            assertThatThrownBy(() -> agendamento.realizar(ordem))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CONFIRMADOS");
        }
    }
}
