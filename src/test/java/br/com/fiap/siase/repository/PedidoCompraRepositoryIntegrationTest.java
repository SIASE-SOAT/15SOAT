package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Peca;
import br.com.fiap.siase.model.PedidoCompra;
import br.com.fiap.siase.model.enums.StatusPedidoCompra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PedidoCompraRepository - Integração")
class PedidoCompraRepositoryIntegrationTest {

    @Autowired
    private PedidoCompraRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private Peca filtro;
    private Peca vela;

    @BeforeEach
    void setUp() {
        filtro = peca("FILTRO-001", "Filtro de Óleo");
        vela   = peca("VELA-001",   "Vela de Ignição");
        entityManager.persistAndFlush(filtro);
        entityManager.persistAndFlush(vela);

        PedidoCompra p1 = pedido(filtro, 10, StatusPedidoCompra.PENDENTE);
        PedidoCompra p2 = pedido(filtro, 5,  StatusPedidoCompra.APROVADO);
        PedidoCompra p3 = pedido(vela,   20, StatusPedidoCompra.PENDENTE);
        repository.saveAll(List.of(p1, p2, p3));
        entityManager.flush();
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("Deve filtrar pedidos por status PENDENTE")
        void deveFiltrarPedidosPendentes() {
            List<PedidoCompra> resultado = repository.findByStatus(StatusPedidoCompra.PENDENTE);

            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Deve filtrar pedidos por status APROVADO")
        void deveFiltrarPedidosAprovados() {
            List<PedidoCompra> resultado = repository.findByStatus(StatusPedidoCompra.APROVADO);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getQuantidadeSolicitada()).isEqualTo(5);
        }

        @Test
        @DisplayName("Deve retornar vazio para status sem pedidos")
        void deveRetornarVazioParaStatusSemPedidos() {
            List<PedidoCompra> resultado = repository.findByStatus(StatusPedidoCompra.RECEBIDO);

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPecaId")
    class FindByPecaId {

        @Test
        @DisplayName("Deve listar todos os pedidos de uma peça")
        void deveListarPedidosDaPeca() {
            List<PedidoCompra> resultado = repository.findByPecaId(filtro.getId());

            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Deve retornar vazio para peça sem pedidos")
        void deveRetornarVazioParaPecaSemPedidos() {
            Peca outra = peca("ROLAM-001", "Rolamento");
            entityManager.persistAndFlush(outra);

            List<PedidoCompra> resultado = repository.findByPecaId(outra.getId());

            assertThat(resultado).isEmpty();
        }
    }

    private Peca peca(String codigo, String nome) {
        Peca p = new Peca();
        p.setCodigo(codigo);
        p.setNome(nome);
        p.setPreco(new BigDecimal("50.00"));
        p.setQuantidadeEstoque(0);
        p.setEstoqueMinimo(5);
        p.setUnidadeMedida("UN");
        p.setAtivo(true);
        return p;
    }

    private PedidoCompra pedido(Peca peca, int quantidade, StatusPedidoCompra status) {
        PedidoCompra p = new PedidoCompra();
        p.setPeca(peca);
        p.setQuantidadeSolicitada(quantidade);
        p.setStatus(status);
        return p;
    }
}
