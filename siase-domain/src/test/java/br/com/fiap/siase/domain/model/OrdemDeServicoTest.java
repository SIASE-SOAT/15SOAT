package br.com.fiap.siase.domain.model;

import br.com.fiap.siase.domain.enums.StatusOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrdemDeServico: fluxo de status e regras de negocio")
class OrdemDeServicoTest {

    private OrdemDeServico os;

    @BeforeEach
    void setUp() {
        os = OrdemDeServico.builder()
                .numero("OS-20260530-ABC123")
                .status(StatusOS.RECEBIDA)
                .build();
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {
            "RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO",
            "APROVADO", "EM_EXECUCAO", "FINALIZADA"
    })
    @DisplayName("Deve avancar status corretamente no fluxo feliz")
    void deveAvancarStatusCorretamente(StatusOS inicial) {
        os.setStatus(inicial);

        os.avancarStatus();

        assertThat(os.getStatus()).isNotEqualTo(inicial);
        assertThat(os.getStatus()).isNotEqualTo(StatusOS.CANCELADA);
    }

    @Test
    @DisplayName("RECEBIDA -> EM_DIAGNOSTICO")
    void recebidaAvançaParaDiagnostico() {
        os.avancarStatus();
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
    }

    @Test
    @DisplayName("EM_DIAGNOSTICO -> AGUARDANDO_APROVACAO")
    void emDiagnosticoAvançaParaAguardandoAprovacao() {
        os.setStatus(StatusOS.EM_DIAGNOSTICO);
        os.avancarStatus();
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
    }

    @Test
    @DisplayName("EM_EXECUCAO -> FINALIZADA")
    void emExecucaoAvançaParaFinalizada() {
        os.setStatus(StatusOS.EM_EXECUCAO);
        os.avancarStatus();
        assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);
    }

    @Test
    @DisplayName("FINALIZADA -> ENTREGUE registra data de fechamento")
    void finalizadaAvançaParaEntregueComDataFechamento() {
        os.setStatus(StatusOS.FINALIZADA);
        os.avancarStatus();
        assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);
        assertThat(os.getDataFechamento()).isNotNull();
    }

    @Test
    @DisplayName("ENTREGUE nao pode avancar")
    void entregueNaoPodeAvancar() {
        os.setStatus(StatusOS.ENTREGUE);
        assertThatThrownBy(() -> os.avancarStatus())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("entregue");
    }

    @Test
    @DisplayName("CANCELADA nao pode avancar")
    void canceladaNaoPodeAvancar() {
        os.setStatus(StatusOS.CANCELADA);
        assertThatThrownBy(() -> os.avancarStatus())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    @DisplayName("Pode cancelar EM_DIAGNOSTICO")
    void podeCancelarEmDiagnostico() {
        os.setStatus(StatusOS.EM_DIAGNOSTICO);
        os.cancelar();
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        assertThat(os.getDataFechamento()).isNotNull();
    }

    @Test
    @DisplayName("Pode cancelar AGUARDANDO_APROVACAO")
    void podeCancelarAguardandoAprovacao() {
        os.setStatus(StatusOS.AGUARDANDO_APROVACAO);
        os.cancelar();
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
    }

    @Test
    @DisplayName("Pode cancelar APROVADO")
    void podeCancelarAprovado() {
        os.setStatus(StatusOS.APROVADO);
        os.cancelar();
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
    }

    @Test
    @DisplayName("NAO pode cancelar EM_EXECUCAO")
    void naoPodeCancelarEmExecucao() {
        os.setStatus(StatusOS.EM_EXECUCAO);
        assertThatThrownBy(() -> os.cancelar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("em execu");
    }

    @Test
    @DisplayName("NAO pode cancelar FINALIZADA")
    void naoPodeCancelarFinalizada() {
        os.setStatus(StatusOS.FINALIZADA);
        assertThatThrownBy(() -> os.cancelar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("finalizada");
    }

    @Test
    @DisplayName("NAO pode cancelar ENTREGUE")
    void naoPodeCancelarEntregue() {
        os.setStatus(StatusOS.ENTREGUE);
        assertThatThrownBy(() -> os.cancelar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("entregue");
    }

    @Test
    @DisplayName("NAO pode cancelar duas vezes")
    void naoPodeCancelarDuasVezes() {
        os.setStatus(StatusOS.EM_DIAGNOSTICO);
        os.cancelar();
        assertThatThrownBy(() -> os.cancelar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    @DisplayName("recalcularTotais soma servicos e pecas corretamente")
    void recalcularTotaisSomaCorretamente() {
        Servico servico = new Servico();
        servico.setPreco(new BigDecimal("150.00"));

        var itemServico = new ItemServico();
        itemServico.setServico(servico);
        itemServico.setPrecoUnitario(new BigDecimal("150.00"));
        os.getItensServico().add(itemServico);

        os.recalcularTotais();

        assertThat(os.getTotalServicos()).isEqualByComparingTo("150.00");
        assertThat(os.getTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Status inicial deve ser RECEBIDA")
    void statusInicialRecebida() {
        var novaOs = new OrdemDeServico();
        assertThat(novaOs.getStatus()).isEqualTo(StatusOS.RECEBIDA);
    }
}
