package br.com.fiap.siase.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ItemServico - Regras de Execução")
class ItemServicoEntityTest {

    private ItemServico item;

    @BeforeEach
    void setUp() {
        item = new ItemServico();
        item.setPrecoUnitario(new BigDecimal("150.00"));
    }

    @Nested
    @DisplayName("Iniciar Execução")
    class IniciarExecucao {

        @Test
        @DisplayName("Deve registrar data de início ao iniciar")
        void deveRegistrarDataInicio() {
            item.iniciarExecucao();

            assertThat(item.getDataInicioExecucao()).isNotNull();
        }

        @Test
        @DisplayName("Não deve sobrescrever data de início se já iniciado")
        void naoDeveSobrescreverDataInicio() {
            item.iniciarExecucao();
            var dataOriginal = item.getDataInicioExecucao();

            item.iniciarExecucao();

            assertThat(item.getDataInicioExecucao()).isEqualTo(dataOriginal);
        }
    }

    @Nested
    @DisplayName("Finalizar Execução")
    class FinalizarExecucao {

        @Test
        @DisplayName("Deve registrar data de fim ao finalizar")
        void deveRegistrarDataFim() {
            item.iniciarExecucao();
            item.finalizarExecucao();

            assertThat(item.getDataFimExecucao()).isNotNull();
        }

        @Test
        @DisplayName("Deve iniciar automaticamente se não iniciado antes de finalizar")
        void deveIniciarAutomaticamenteAoFinalizar() {
            item.finalizarExecucao();

            assertThat(item.getDataInicioExecucao()).isNotNull();
            assertThat(item.getDataFimExecucao()).isNotNull();
        }

        @Test
        @DisplayName("Não deve sobrescrever data de fim se já finalizado")
        void naoDeveSobrescreverDataFim() {
            item.iniciarExecucao();
            item.finalizarExecucao();
            var dataOriginal = item.getDataFimExecucao();

            item.finalizarExecucao();

            assertThat(item.getDataFimExecucao()).isEqualTo(dataOriginal);
        }
    }
}
