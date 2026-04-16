package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.PedidoCompraRequest;
import br.com.fiap.siase.dto.response.PedidoCompraResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Peca;
import br.com.fiap.siase.model.PedidoCompra;
import br.com.fiap.siase.model.enums.StatusPedidoCompra;
import br.com.fiap.siase.repository.PecaRepository;
import br.com.fiap.siase.repository.PedidoCompraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoCompraService - Regras de Negócio")
class PedidoCompraServiceTest {

    @Mock
    private PedidoCompraRepository repository;

    @Mock
    private PecaRepository pecaRepository;

    @InjectMocks
    private PedidoCompraService pedidoCompraService;

    private Peca peca;
    private PedidoCompra pedido;
    private UUID pecaId;
    private UUID pedidoId;

    @BeforeEach
    void setUp() {
        pecaId = UUID.randomUUID();
        pedidoId = UUID.randomUUID();

        peca = new Peca();
        ReflectionTestUtils.setField(peca, "id", pecaId);
        peca.setCodigo("FILTRO-001");
        peca.setNome("Filtro de Óleo");
        peca.setPreco(new BigDecimal("35.00"));
        peca.setQuantidadeEstoque(5);
        peca.setEstoqueMinimo(10);
        peca.setUnidadeMedida("UN");
        peca.setAtivo(true);

        pedido = new PedidoCompra();
        ReflectionTestUtils.setField(pedido, "id", pedidoId);
        ReflectionTestUtils.setField(pedido, "criadoEm", LocalDateTime.now());
        ReflectionTestUtils.setField(pedido, "atualizadoEm", LocalDateTime.now());
        pedido.setPeca(peca);
        pedido.setQuantidadeSolicitada(20);
    }

    // -----------------------------------------------------------------------
    // CRIAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Criar pedido de compra")
    class Criar {

        @Test
        @DisplayName("Deve criar pedido de compra com sucesso")
        void deveCriarComSucesso() {
            when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(peca));
            when(repository.save(any(PedidoCompra.class))).thenReturn(pedido);

            PedidoCompraResponse response = pedidoCompraService.criar(
                    new PedidoCompraRequest(pecaId, 20, "Reposição urgente"));

            assertThat(response).isNotNull();
            assertThat(response.pecaId()).isEqualTo(pecaId);
            assertThat(response.quantidadeSolicitada()).isEqualTo(20);
            verify(repository).save(any(PedidoCompra.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando peça não encontrada")
        void deveLancarExcecaoQuandoPecaNaoEncontrada() {
            when(pecaRepository.findById(pecaId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoCompraService.criar(
                    new PedidoCompraRequest(pecaId, 20, null)))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // LISTAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Listar pedidos de compra")
    class Listar {

        @Test
        @DisplayName("Deve listar todos os pedidos")
        void deveListarTodos() {
            when(repository.findAll()).thenReturn(List.of(pedido));

            List<PedidoCompraResponse> lista = pedidoCompraService.listar();

            assertThat(lista).hasSize(1);
            assertThat(lista.get(0).pecaCodigo()).isEqualTo("FILTRO-001");
        }

        @Test
        @DisplayName("Deve listar pedidos por status")
        void deveListarPorStatus() {
            when(repository.findByStatus(StatusPedidoCompra.PENDENTE)).thenReturn(List.of(pedido));

            List<PedidoCompraResponse> lista = pedidoCompraService.listarPorStatus(StatusPedidoCompra.PENDENTE);

            assertThat(lista).hasSize(1);
            assertThat(lista.get(0).status()).isEqualTo("PENDENTE");
        }

        @Test
        @DisplayName("Deve buscar pedido por ID")
        void deveBuscarPorId() {
            when(repository.findById(pedidoId)).thenReturn(Optional.of(pedido));

            PedidoCompraResponse response = pedidoCompraService.buscarPorId(pedidoId);

            assertThat(response.id()).isEqualTo(pedidoId);
        }

        @Test
        @DisplayName("Deve lançar exceção para pedido inexistente")
        void deveLancarExcecaoPorIdInexistente() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoCompraService.buscarPorId(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // APROVAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Aprovar pedido de compra")
    class Aprovar {

        @Test
        @DisplayName("Deve aprovar pedido pendente")
        void deveAprovarPedidoPendente() {
            when(repository.findById(pedidoId)).thenReturn(Optional.of(pedido));
            when(repository.save(any(PedidoCompra.class))).thenReturn(pedido);

            PedidoCompraResponse response = pedidoCompraService.aprovar(pedidoId);

            assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.APROVADO);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Deve lançar BusinessException ao aprovar pedido já aprovado")
        void deveLancarExcecaoAoAprovarJaAprovado() {
            pedido.aprovar(); // PENDENTE -> APROVADO
            when(repository.findById(pedidoId)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoCompraService.aprovar(pedidoId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PENDENTES podem ser aprovados");
        }
    }

    // -----------------------------------------------------------------------
    // RECEBER
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Receber pedido de compra")
    class Receber {

        @Test
        @DisplayName("Deve receber pedido aprovado e atualizar estoque")
        void deveReceberPedidoAprovado() {
            pedido.aprovar(); // PENDENTE -> APROVADO
            when(repository.findById(pedidoId)).thenReturn(Optional.of(pedido));
            when(repository.save(any(PedidoCompra.class))).thenReturn(pedido);
            when(pecaRepository.save(any(Peca.class))).thenReturn(peca);

            PedidoCompraResponse response = pedidoCompraService.receber(pedidoId, 20);

            assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.RECEBIDO);
            assertThat(pedido.getQuantidadeRecebida()).isEqualTo(20);
            verify(pecaRepository).save(peca);
        }

        @Test
        @DisplayName("Deve lançar BusinessException para quantidade zero")
        void deveLancarExcecaoParaQuantidadeZero() {
            when(repository.findById(pedidoId)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoCompraService.receber(pedidoId, 0))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("maior que zero");
        }

        @Test
        @DisplayName("Deve lançar BusinessException ao receber pedido não aprovado")
        void deveLancarExcecaoAoReceberNaoAprovado() {
            // pedido está PENDENTE (não aprovado)
            when(repository.findById(pedidoId)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoCompraService.receber(pedidoId, 10))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("APROVADOS podem ser recebidos");
        }
    }

    // -----------------------------------------------------------------------
    // CANCELAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cancelar pedido de compra")
    class Cancelar {

        @Test
        @DisplayName("Deve cancelar pedido pendente")
        void deveCancelarPedidoPendente() {
            when(repository.findById(pedidoId)).thenReturn(Optional.of(pedido));
            when(repository.save(any(PedidoCompra.class))).thenReturn(pedido);

            PedidoCompraResponse response = pedidoCompraService.cancelar(pedidoId);

            assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.CANCELADO);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Deve cancelar pedido aprovado")
        void deveCancelarPedidoAprovado() {
            pedido.aprovar();
            when(repository.findById(pedidoId)).thenReturn(Optional.of(pedido));
            when(repository.save(any(PedidoCompra.class))).thenReturn(pedido);

            pedidoCompraService.cancelar(pedidoId);

            assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.CANCELADO);
        }

        @Test
        @DisplayName("Deve lançar BusinessException ao cancelar pedido já recebido")
        void deveLancarExcecaoAoCancelarRecebido() {
            pedido.aprovar();
            pedido.receber(20); // APROVADO -> RECEBIDO
            when(repository.findById(pedidoId)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoCompraService.cancelar(pedidoId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já recebido não pode ser cancelado");
        }
    }
}
