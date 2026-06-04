package br.com.fiap.siase.domain.model;

import br.com.fiap.siase.domain.enums.StatusPedidoCompra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PedidoCompra: regras de negocio")
class PedidoCompraTest {

    private PedidoCompra pedido;
    private Peca peca;

    @BeforeEach
    void setUp() {
        peca = new Peca();
        peca.setCodigo("FILTRO-01");
        peca.setNome("Filtro de Oleo");
        peca.setPreco(new BigDecimal("45.90"));
        peca.setQuantidadeEstoque(5);
        peca.setEstoqueMinimo(1);
        peca.setUnidadeMedida("UN");
        peca.setAtivo(true);

        pedido = PedidoCompra.builder()
                .peca(peca)
                .quantidadeSolicitada(10)
                .build();
    }

    @Test
    @DisplayName("Status inicial deve ser PENDENTE")
    void statusInicialDevePendente() {
        assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.PENDENTE);
        assertThat(pedido.getQuantidadeRecebida()).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve aprovar pedido pendente")
    void deveAprovarPedidoPendente() {
        pedido.aprovar();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.APROVADO);
    }

    @Test
    @DisplayName("Nao deve aprovar pedido nao pendente")
    void naoDeveAprovarPedidoNaoPendente() {
        pedido.aprovar();
        assertThatThrownBy(() -> pedido.aprovar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDENTES");
    }

    @Test
    @DisplayName("Deve receber pedido aprovado e atualizar estoque")
    void deveReceberPedidoAprovado() {
        pedido.aprovar();
        pedido.receber(10);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.RECEBIDO);
        assertThat(pedido.getQuantidadeRecebida()).isEqualTo(10);
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(15);
    }

    @Test
    @DisplayName("Nao deve receber pedido nao aprovado")
    void naoDeveReceberPedidoNaoAprovado() {
        assertThatThrownBy(() -> pedido.receber(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APROVADOS");
    }

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
    @DisplayName("Nao deve cancelar pedido ja recebido")
    void naoDeveCancelarPedidoJaRecebido() {
        pedido.aprovar();
        pedido.receber(10);
        assertThatThrownBy(() -> pedido.cancelar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("recebido");
    }
}
