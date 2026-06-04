package br.com.fiap.siase.domain.model;

import br.com.fiap.siase.domain.enums.StatusAgendamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agendamento: regras de negocio")
class AgendamentoTest {

    private Agendamento agendamento;

    @BeforeEach
    void setUp() {
        agendamento = Agendamento.builder()
                .dataHora(LocalDateTime.now().plusDays(1))
                .descricaoServicos("Revisao geral")
                .build();
    }

    @Test
    @DisplayName("Status inicial deve ser AGENDADO")
    void statusInicialDeveSerAgendado() {
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
    }

    @Test
    @DisplayName("Deve confirmar agendamento com status AGENDADO")
    void deveConfirmarAgendamento() {
        agendamento.confirmar();
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    @DisplayName("Nao deve confirmar agendamento ja confirmado")
    void naoDeveConfirmarAgendamentoJaConfirmado() {
        agendamento.confirmar();
        assertThatThrownBy(() -> agendamento.confirmar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AGENDADO");
    }

    @Test
    @DisplayName("Deve cancelar agendamento com status AGENDADO")
    void deveCancelarAgendamentoAgendado() {
        agendamento.cancelar();
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    @DisplayName("Deve cancelar agendamento com status CONFIRMADO")
    void deveCancelarAgendamentoConfirmado() {
        agendamento.confirmar();
        agendamento.cancelar();
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    @DisplayName("Nao deve cancelar agendamento ja realizado")
    void naoDeveCancelarAgendamentoRealizado() {
        agendamento.confirmar();
        agendamento.realizar(new OrdemDeServico());
        assertThatThrownBy(() -> agendamento.cancelar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("realizado");
    }

    @Test
    @DisplayName("Deve realizar agendamento confirmado e associar OS")
    void deveRealizarAgendamentoConfirmado() {
        OrdemDeServico os = OrdemDeServico.builder().numero("OS-20260101-ABC123").build();
        agendamento.confirmar();
        agendamento.realizar(os);
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.REALIZADO);
        assertThat(agendamento.getOrdemDeServico()).isEqualTo(os);
    }

    @Test
    @DisplayName("Nao deve realizar agendamento nao confirmado")
    void naoDeveRealizarAgendamentoNaoConfirmado() {
        OrdemDeServico os = new OrdemDeServico();
        assertThatThrownBy(() -> agendamento.realizar(os))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFIRMADOS");
    }
}
