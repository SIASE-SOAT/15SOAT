package br.com.fiap.siase.model;

import br.com.fiap.siase.model.enums.StatusPedidoCompra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PedidoCompra - Regras de Domínio")
class PedidoCompraEntityTest {

    private PedidoCompra pedido;
    private Peca peca;

    @BeforeEach
    void setUp() {
        peca = new Peca();
        peca.setCodigo("FILTRO-001");
        peca.setNome("Filtro de Óleo");
        peca.setPreco(new BigDecimal("50.00"));
        peca.setQuantidadeEstoque(0);
        peca.setEstoqueMinimo(5);
        peca.setUnidadeMedida("UN");
        peca.setAtivo(true);

        pedido = new PedidoCompra();
        pedido.setPeca(peca);
        pedido.setQuantidadeSolicitada(10);
    }

    @Nested
    @DisplayName("Aprovar")
    class Aprovar {

        @Test
        @DisplayName("Deve aprovar pedido pendente")
        void deveAprovarPedidoPendente() {
            pedido.aprovar();

            assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.APROVADO);
        }

        @Test
        @DisplayName("Deve rejeitar aprovação de pedido não-pendente")
        void deveRejeitarAprovacaoDeNaoPendente() {
            pedido.aprovar();

            assertThatThrownBy(() -> pedido.aprovar())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDENTES podem ser aprovados");
        }

        @Test
        @DisplayName("Deve rejeitar aprovação de pedido cancelado")
        void deveRejeitarAprovacaoDeCancelado() {
            pedido.cancelar();

            assertThatThrownBy(() -> pedido.aprovar())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Receber")
    class Receber {

        @Test
        @DisplayName("Deve receber pedido aprovado e atualizar estoque")
        void deveReceberPedidoAprovado() {
            pedido.aprovar();
            pedido.receber(10);

            assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.RECEBIDO);
            assertThat(pedido.getQuantidadeRecebida()).isEqualTo(10);
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(10);
        }

        @Test
        @DisplayName("Deve rejeitar recebimento de pedido não-aprovado")
        void deveRejeitarRecebimentoDeNaoAprovado() {
            assertThatThrownBy(() -> pedido.receber(10))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("APROVADOS podem ser recebidos");
        }
    }

    @Nested
    @DisplayName("Cancelar")
    class Cancelar {

        @Test
        @DisplayName("Deve cancelar pedido pendente")
        void deveCancelarPedidoPendente() {
            pedido.cancelar();

            assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.CANCELADO);
        }

        @Test
        @DisplayName("Deve cancelar pedido aprovado")
        void deveCancelarPedidoAprovado() {
            pedido.aprovar();
            pedido.cancelar();

            assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.CANCELADO);
        }

        @Test
        @DisplayName("Deve rejeitar cancelamento de pedido já recebido")
        void deveRejeitarCancelamentoDeRecebido() {
            pedido.aprovar();
            pedido.receber(10);

            assertThatThrownBy(() -> pedido.cancelar())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("já recebido não pode ser cancelado");
        }
    }
}
