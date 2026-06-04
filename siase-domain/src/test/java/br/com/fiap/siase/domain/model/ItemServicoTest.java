package br.com.fiap.siase.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ItemServico: execucao de servico")
class ItemServicoTest {

    private ItemServico item;

    @BeforeEach
    void setUp() {
        item = ItemServico.builder()
                .precoUnitario(new BigDecimal("150.00"))
                .tempoEstimadoMinutos(60)
                .build();
    }

    @Test
    @DisplayName("Estado inicial sem datas de execucao")
    void estadoInicialSemDatasDeExecucao() {
        assertThat(item.getDataInicioExecucao()).isNull();
        assertThat(item.getDataFimExecucao()).isNull();
    }

    @Test
    @DisplayName("Deve registrar data de inicio ao iniciar execucao")
    void deveRegistrarDataInicioAoIniciar() {
        item.iniciarExecucao();
        assertThat(item.getDataInicioExecucao()).isNotNull();
    }

    @Test
    @DisplayName("Nao deve sobrescrever data de inicio se ja iniciado")
    void naoDeveSobrescreverDataInicioSeJaIniciado() {
        item.iniciarExecucao();
        var primeiroInicio = item.getDataInicioExecucao();
        item.iniciarExecucao();
        assertThat(item.getDataInicioExecucao()).isEqualTo(primeiroInicio);
    }

    @Test
    @DisplayName("Deve registrar data de fim ao finalizar execucao")
    void deveRegistrarDataFimAoFinalizar() {
        item.iniciarExecucao();
        item.finalizarExecucao();
        assertThat(item.getDataFimExecucao()).isNotNull();
    }

    @Test
    @DisplayName("Deve iniciar automaticamente ao finalizar sem ter iniciado")
    void deveIniciarAutomaticamenteAoFinalizar() {
        item.finalizarExecucao();
        assertThat(item.getDataInicioExecucao()).isNotNull();
        assertThat(item.getDataFimExecucao()).isNotNull();
    }

    @Test
    @DisplayName("Nao deve sobrescrever data de fim se ja finalizado")
    void naoDeveSobrescreverDataFimSeJaFinalizado() {
        item.finalizarExecucao();
        var primeiroFim = item.getDataFimExecucao();
        item.finalizarExecucao();
        assertThat(item.getDataFimExecucao()).isEqualTo(primeiroFim);
    }
}
