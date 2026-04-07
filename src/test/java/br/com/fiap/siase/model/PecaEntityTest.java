package br.com.fiap.siase.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Peca - Regras de Negócio de Estoque")
class PecaEntityTest {

    private Peca peca;

    @BeforeEach
    void setUp() {
        peca = new Peca();
        peca.setNome("Filtro de Óleo");
        peca.setCodigo("FILTRO-001");
        peca.setQuantidadeEstoque(10);
        peca.setUnidadeMedida("UN");
    }

    @Nested
    @DisplayName("Verificação de disponibilidade")
    class VerificacaoDeEstoque {

        @Test
        @DisplayName("Deve ter estoque quando quantidade disponível é suficiente")
        void deveRetornarTrueQuandoEstoqueSuficiente() {
            assertThat(peca.temEstoque(5)).isTrue();
        }

        @Test
        @DisplayName("Deve ter estoque quando quantidade solicitada é exatamente o disponível")
        void deveRetornarTrueQuandoEstoqueExato() {
            assertThat(peca.temEstoque(10)).isTrue();
        }

        @Test
        @DisplayName("Não deve ter estoque quando quantidade solicitada excede o disponível")
        void deveRetornarFalseQuandoEstoqueInsuficiente() {
            assertThat(peca.temEstoque(11)).isFalse();
        }

        @Test
        @DisplayName("Não deve ter estoque quando estoque é zero")
        void deveRetornarFalseQuandoEstoqueZero() {
            peca.setQuantidadeEstoque(0);
            assertThat(peca.temEstoque(1)).isFalse();
        }
    }

    @Nested
    @DisplayName("Reserva de estoque")
    class ReservaDeEstoque {

        @Test
        @DisplayName("Deve decrementar o estoque ao reservar quantidade parcial")
        void deveDecrementarEstoqueAoReservar() {
            peca.reservarEstoque(3);
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(7);
        }

        @Test
        @DisplayName("Deve zerar estoque ao reservar quantidade total")
        void deveReservarEstoqueTotal() {
            peca.reservarEstoque(10);
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(0);
        }

        @Test
        @DisplayName("Deve lançar exceção com nome da peça e estoque disponível quando insuficiente")
        void deveLancarExcecaoQuandoEstoqueInsuficiente() {
            assertThatThrownBy(() -> peca.reservarEstoque(11))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Estoque insuficiente")
                    .hasMessageContaining("Filtro de Óleo")
                    .hasMessageContaining("10");
        }

        @Test
        @DisplayName("Deve lançar exceção quando estoque já está zerado")
        void deveLancarExcecaoQuandoEstoqueZero() {
            peca.setQuantidadeEstoque(0);
            assertThatThrownBy(() -> peca.reservarEstoque(1))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Não deve alterar estoque quando lança exceção")
        void naoDeveAlterarEstoqueEmExcecao() {
            int estoqueOriginal = peca.getQuantidadeEstoque();

            assertThatThrownBy(() -> peca.reservarEstoque(99))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(peca.getQuantidadeEstoque()).isEqualTo(estoqueOriginal);
        }
    }

    @Nested
    @DisplayName("Devolução de estoque")
    class DevolucaoDeEstoque {

        @Test
        @DisplayName("Deve incrementar o estoque ao devolver")
        void deveIncrementarEstoqueAoDevolver() {
            peca.devolverEstoque(5);
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(15);
        }

        @Test
        @DisplayName("Deve restaurar estoque original após reserva e devolução")
        void deveDevolverAposReserva() {
            peca.reservarEstoque(3);
            peca.devolverEstoque(3);
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(10);
        }

        @Test
        @DisplayName("Deve permitir devolução mesmo com estoque zerado")
        void deveDevolverComEstoqueZerado() {
            peca.setQuantidadeEstoque(0);
            peca.devolverEstoque(5);
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(5);
        }
    }
}
