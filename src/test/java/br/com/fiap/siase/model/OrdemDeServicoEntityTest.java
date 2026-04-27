package br.com.fiap.siase.model;

import br.com.fiap.siase.model.enums.StatusOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrdemDeServico - Regras de Negócio")
class OrdemDeServicoEntityTest {

    private OrdemDeServico os;

    @BeforeEach
    void setUp() {
        os = new OrdemDeServico();
        os.setStatus(StatusOS.RECEBIDA);
        os.setTotalServicos(BigDecimal.ZERO);
        os.setTotalPecas(BigDecimal.ZERO);
        os.setTotal(BigDecimal.ZERO);
    }

    @Nested
    @DisplayName("Progressão de Status")
    class ProgressaoDeStatus {

        @Test
        @DisplayName("RECEBIDA → EM_DIAGNOSTICO")
        void deveAvancarDeRecebidaParaEmDiagnostico() {
            os.avancarStatus();
            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        }

        @Test
        @DisplayName("EM_DIAGNOSTICO → AGUARDANDO_APROVACAO")
        void deveAvancarDeEmDiagnosticoParaAguardandoAprovacao() {
            os.setStatus(StatusOS.EM_DIAGNOSTICO);
            os.avancarStatus();
            assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        }

        @Test
        @DisplayName("AGUARDANDO_APROVACAO → APROVADO (cliente aprova)")
        void deveAvancarDeAguardandoAprovacaoParaAprovado() {
            os.setStatus(StatusOS.AGUARDANDO_APROVACAO);
            os.avancarStatus();
            assertThat(os.getStatus()).isEqualTo(StatusOS.APROVADO);
        }

        @Test
        @DisplayName("APROVADO → EM_EXECUCAO (mecânico inicia)")
        void deveAvancarDeAprovadoParaEmExecucao() {
            os.setStatus(StatusOS.APROVADO);
            os.avancarStatus();
            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        }

        @Test
        @DisplayName("EM_EXECUCAO → FINALIZADA")
        void deveAvancarDeEmExecucaoParaFinalizada() {
            os.setStatus(StatusOS.EM_EXECUCAO);
            os.avancarStatus();
            assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);
        }

        @Test
        @DisplayName("FINALIZADA → ENTREGUE")
        void deveAvancarDeFinalizadaParaEntregue() {
            os.setStatus(StatusOS.FINALIZADA);
            os.avancarStatus();
            assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);
        }

        @Test
        @DisplayName("ENTREGUE → lança IllegalStateException")
        void deveLancarExcecaoAoAvancarDeEntregue() {
            os.setStatus(StatusOS.ENTREGUE);
            assertThatThrownBy(() -> os.avancarStatus())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Ordem de serviço já foi entregue.");
        }

        @Test
        @DisplayName("dataFechamento deve ser preenchida ao entregar")
        void deveDefinirDataFechamentoAoSerEntregue() {
            os.setStatus(StatusOS.FINALIZADA);
            assertThat(os.getDataFechamento()).isNull();

            os.avancarStatus();

            assertThat(os.getDataFechamento()).isNotNull();
        }

        @Test
        @DisplayName("dataFechamento não deve ser preenchida em transições intermediárias")
        void naoDeveDefinirDataFechamentoEmTransicoesAnteriores() {
            os.avancarStatus(); // RECEBIDA → EM_DIAGNOSTICO
            assertThat(os.getDataFechamento()).isNull();

            os.avancarStatus(); // EM_DIAGNOSTICO → AGUARDANDO_APROVACAO
            assertThat(os.getDataFechamento()).isNull();

            os.avancarStatus(); // AGUARDANDO_APROVACAO → EM_EXECUCAO
            assertThat(os.getDataFechamento()).isNull();

            os.avancarStatus(); // EM_EXECUCAO → FINALIZADA
            assertThat(os.getDataFechamento()).isNull();
        }
    }

    @Nested
    @DisplayName("Cálculo de Totais")
    class CalculoDeTotais {

        @Test
        @DisplayName("Total zero quando sem itens")
        void deveTerTotalZeroSemItens() {
            os.recalcularTotais();

            assertThat(os.getTotalServicos()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(os.getTotalPecas()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(os.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Deve somar serviços corretamente")
        void deveSomarServicos() {
            os.getItensServico().add(itemServico(new BigDecimal("100.00")));
            os.getItensServico().add(itemServico(new BigDecimal("50.00")));

            os.recalcularTotais();

            assertThat(os.getTotalServicos()).isEqualByComparingTo("150.00");
            assertThat(os.getTotalPecas()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(os.getTotal()).isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("Deve multiplicar quantidade x preço nas peças")
        void deveSomarPecasComQuantidade() {
            os.getItensPeca().add(itemPeca(new BigDecimal("25.00"), 3)); // R$ 75
            os.getItensPeca().add(itemPeca(new BigDecimal("10.00"), 2)); // R$ 20

            os.recalcularTotais();

            assertThat(os.getTotalPecas()).isEqualByComparingTo("95.00");
            assertThat(os.getTotalServicos()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(os.getTotal()).isEqualByComparingTo("95.00");
        }

        @Test
        @DisplayName("Total é a soma de serviços e peças")
        void totalDeveSerSomaDeServicosEPecas() {
            os.getItensServico().add(itemServico(new BigDecimal("200.00")));
            os.getItensPeca().add(itemPeca(new BigDecimal("30.00"), 2)); // R$ 60

            os.recalcularTotais();

            assertThat(os.getTotalServicos()).isEqualByComparingTo("200.00");
            assertThat(os.getTotalPecas()).isEqualByComparingTo("60.00");
            assertThat(os.getTotal()).isEqualByComparingTo("260.00");
        }

        @Test
        @DisplayName("Deve recalcular corretamente após adicionar novo item")
        void deveAtualizarTotalAposNovoItem() {
            os.getItensServico().add(itemServico(new BigDecimal("200.00")));
            os.recalcularTotais();
            assertThat(os.getTotal()).isEqualByComparingTo("200.00");

            os.getItensPeca().add(itemPeca(new BigDecimal("50.00"), 1));
            os.recalcularTotais();
            assertThat(os.getTotal()).isEqualByComparingTo("250.00");
        }
    }

    private ItemServico itemServico(BigDecimal preco) {
        ItemServico item = new ItemServico();
        item.setPrecoUnitario(preco);
        item.setOrdemDeServico(os);
        return item;
    }

    private ItemPeca itemPeca(BigDecimal precoUnitario, int quantidade) {
        ItemPeca item = new ItemPeca();
        item.setPrecoUnitario(precoUnitario);
        item.setQuantidade(quantidade);
        item.setOrdemDeServico(os);
        return item;
    }
}
