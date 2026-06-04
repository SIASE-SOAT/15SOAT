package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.PedidoCompraRequest;
import br.com.fiap.siase.domain.enums.StatusPedidoCompra;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Peca;
import br.com.fiap.siase.domain.model.PedidoCompra;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import br.com.fiap.siase.domain.port.PedidoCompraRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PedidoCompraController - Endpoints REST")
class PedidoCompraControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PedidoCompraRepositoryPort pedidoCompraRepository;
    @MockBean PecaRepositoryPort pecaRepository;

    private static final UUID PEDIDO_ID = UUID.fromString("00000000-0000-0000-0000-000000000300");
    private static final UUID PECA_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private Peca peca;
    private PedidoCompra pedidoPendente;
    private PedidoCompra pedidoAprovado;
    private PedidoCompra pedidoRecebido;
    private PedidoCompra pedidoCancelado;

    @BeforeEach
    void setUp() {
        peca = new Peca();
        peca.setId(PECA_ID);
        peca.setCodigo("PECA-001");
        peca.setNome("Filtro de Óleo");
        peca.setDescricao("Filtro de óleo original");
        peca.setPreco(new BigDecimal("45.00"));
        peca.setQuantidadeEstoque(5);

        pedidoPendente = new PedidoCompra();
        pedidoPendente.setId(PEDIDO_ID);
        pedidoPendente.setPeca(peca);
        pedidoPendente.setQuantidadeSolicitada(10);
        pedidoPendente.setQuantidadeRecebida(0);
        pedidoPendente.setStatus(StatusPedidoCompra.PENDENTE);
        pedidoPendente.setObservacoes("Reposição urgente");
        pedidoPendente.setCriadoEm(LocalDateTime.now());
        pedidoPendente.setAtualizadoEm(LocalDateTime.now());

        pedidoAprovado = new PedidoCompra();
        pedidoAprovado.setId(PEDIDO_ID);
        pedidoAprovado.setPeca(peca);
        pedidoAprovado.setQuantidadeSolicitada(10);
        pedidoAprovado.setQuantidadeRecebida(0);
        pedidoAprovado.setStatus(StatusPedidoCompra.APROVADO);
        pedidoAprovado.setObservacoes("Reposição urgente");
        pedidoAprovado.setCriadoEm(LocalDateTime.now());
        pedidoAprovado.setAtualizadoEm(LocalDateTime.now());

        pedidoRecebido = new PedidoCompra();
        pedidoRecebido.setId(PEDIDO_ID);
        pedidoRecebido.setPeca(peca);
        pedidoRecebido.setQuantidadeSolicitada(10);
        pedidoRecebido.setQuantidadeRecebida(5);
        pedidoRecebido.setStatus(StatusPedidoCompra.RECEBIDO);
        pedidoRecebido.setObservacoes("Reposição urgente");
        pedidoRecebido.setCriadoEm(LocalDateTime.now());
        pedidoRecebido.setAtualizadoEm(LocalDateTime.now());

        pedidoCancelado = new PedidoCompra();
        pedidoCancelado.setId(PEDIDO_ID);
        pedidoCancelado.setPeca(peca);
        pedidoCancelado.setQuantidadeSolicitada(10);
        pedidoCancelado.setQuantidadeRecebida(0);
        pedidoCancelado.setStatus(StatusPedidoCompra.CANCELADO);
        pedidoCancelado.setObservacoes(null);
        pedidoCancelado.setCriadoEm(LocalDateTime.now());
        pedidoCancelado.setAtualizadoEm(LocalDateTime.now());
    }

    @Nested
    @DisplayName("POST /pedidos-compra - Criar Pedido")
    class CriarPedido {

        @Test
        @DisplayName("deve criar pedido e retornar 201")
        void deveCriarPedido() throws Exception {
            PedidoCompraRequest request = new PedidoCompraRequest(PECA_ID, 10, "Reposição urgente");

            when(pecaRepository.findById(PECA_ID)).thenReturn(Optional.of(peca));
            when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(invocation -> {
                PedidoCompra p = invocation.getArgument(0);
                p.setId(PEDIDO_ID);
                p.setCriadoEm(LocalDateTime.now());
                p.setAtualizadoEm(LocalDateTime.now());
                return p;
            });

            mockMvc.perform(post("/pedidos-compra")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(PEDIDO_ID.toString()))
                    .andExpect(jsonPath("$.status").value("PENDENTE"))
                    .andExpect(jsonPath("$.pecaNome").value("Filtro de Óleo"));
        }

        @Test
        @DisplayName("deve retornar 404 quando peça não encontrada")
        void deveRetornar404QuandoPecaNaoEncontrada() throws Exception {
            UUID pecaIdInvalida = UUID.randomUUID();
            PedidoCompraRequest request = new PedidoCompraRequest(pecaIdInvalida, 10, null);

            when(pecaRepository.findById(pecaIdInvalida)).thenReturn(Optional.empty());

            mockMvc.perform(post("/pedidos-compra")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 400 quando dados inválidos")
        void deveRetornar400ComDadosInvalidos() throws Exception {
            PedidoCompraRequest request = new PedidoCompraRequest(null, null, null);

            mockMvc.perform(post("/pedidos-compra")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando quantidade é zero")
        void deveRetornar400ComQuantidadeZero() throws Exception {
            PedidoCompraRequest request = new PedidoCompraRequest(PECA_ID, 0, null);

            mockMvc.perform(post("/pedidos-compra")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /pedidos-compra - Listar Pedidos")
    class ListarPedidos {

        @Test
        @DisplayName("deve listar todos os pedidos sem filtro")
        void deveListarTodos() throws Exception {
            when(pedidoCompraRepository.findAll()).thenReturn(List.of(pedidoPendente, pedidoAprovado));

            mockMvc.perform(get("/pedidos-compra"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("deve filtrar por status")
        void deveListarPorStatus() throws Exception {
            when(pedidoCompraRepository.findByStatus(StatusPedidoCompra.PENDENTE))
                    .thenReturn(List.of(pedidoPendente));

            mockMvc.perform(get("/pedidos-compra").param("status", "PENDENTE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("PENDENTE"));
        }

        @Test
        @DisplayName("deve retornar lista vazia")
        void deveRetornarListaVazia() throws Exception {
            when(pedidoCompraRepository.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/pedidos-compra"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /pedidos-compra/{id} - Buscar por ID")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar 200 com pedido")
        void deveRetornarPedido() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.of(pedidoPendente));

            mockMvc.perform(get("/pedidos-compra/{id}", PEDIDO_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PEDIDO_ID.toString()))
                    .andExpect(jsonPath("$.status").value("PENDENTE"))
                    .andExpect(jsonPath("$.pecaCodigo").value("PECA-001"));
        }

        @Test
        @DisplayName("deve retornar 404 quando pedido não encontrado")
        void deveRetornar404() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/pedidos-compra/{id}", PEDIDO_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /pedidos-compra/{id}/aprovar - Aprovar Pedido")
    class AprovarPedido {

        @Test
        @DisplayName("deve aprovar pedido e retornar 200")
        void deveAprovar() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.of(pedidoPendente));
            when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(invocation -> {
                PedidoCompra p = invocation.getArgument(0);
                p.setAtualizadoEm(LocalDateTime.now());
                return p;
            });

            mockMvc.perform(patch("/pedidos-compra/{id}/aprovar", PEDIDO_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APROVADO"));
        }

        @Test
        @DisplayName("deve retornar 404 quando pedido não encontrado")
        void deveRetornar404() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(patch("/pedidos-compra/{id}/aprovar", PEDIDO_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 422 quando pedido não está PENDENTE")
        void deveRetornar422QuandoNaoPendente() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.of(pedidoAprovado));

            mockMvc.perform(patch("/pedidos-compra/{id}/aprovar", PEDIDO_ID))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("PATCH /pedidos-compra/{id}/receber - Receber Pedido")
    class ReceberPedido {

        @Test
        @DisplayName("deve receber pedido e retornar 200")
        void deveReceber() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.of(pedidoAprovado));
            when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(invocation -> {
                PedidoCompra p = invocation.getArgument(0);
                p.setAtualizadoEm(LocalDateTime.now());
                return p;
            });
            when(pecaRepository.save(any(Peca.class))).thenAnswer(invocation -> invocation.getArgument(0));

            mockMvc.perform(patch("/pedidos-compra/{id}/receber", PEDIDO_ID)
                            .param("quantidade", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RECEBIDO"))
                    .andExpect(jsonPath("$.quantidadeRecebida").value(5));
        }

        @Test
        @DisplayName("deve retornar 404 quando pedido não encontrado")
        void deveRetornar404() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(patch("/pedidos-compra/{id}/receber", PEDIDO_ID)
                            .param("quantidade", "5"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 422 quando pedido não está APROVADO")
        void deveRetornar422QuandoNaoAprovado() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.of(pedidoPendente));

            mockMvc.perform(patch("/pedidos-compra/{id}/receber", PEDIDO_ID)
                            .param("quantidade", "5"))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("deve retornar 422 quando pedido já recebido")
        void deveRetornar422QuandoJaRecebido() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.of(pedidoRecebido));

            mockMvc.perform(patch("/pedidos-compra/{id}/receber", PEDIDO_ID)
                            .param("quantidade", "3"))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("PATCH /pedidos-compra/{id}/cancelar - Cancelar Pedido")
    class CancelarPedido {

        @Test
        @DisplayName("deve cancelar pedido pendente e retornar 200")
        void deveCancelarPendente() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.of(pedidoPendente));
            when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(invocation -> {
                PedidoCompra p = invocation.getArgument(0);
                p.setAtualizadoEm(LocalDateTime.now());
                return p;
            });

            mockMvc.perform(patch("/pedidos-compra/{id}/cancelar", PEDIDO_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"));
        }

        @Test
        @DisplayName("deve cancelar pedido aprovado e retornar 200")
        void deveCancelarAprovado() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.of(pedidoAprovado));
            when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(invocation -> {
                PedidoCompra p = invocation.getArgument(0);
                p.setAtualizadoEm(LocalDateTime.now());
                return p;
            });

            mockMvc.perform(patch("/pedidos-compra/{id}/cancelar", PEDIDO_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"));
        }

        @Test
        @DisplayName("deve retornar 404 quando pedido não encontrado")
        void deveRetornar404() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(patch("/pedidos-compra/{id}/cancelar", PEDIDO_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 422 quando pedido já RECEBIDO")
        void deveRetornar422QuandoJaRecebido() throws Exception {
            when(pedidoCompraRepository.findById(PEDIDO_ID))
                    .thenReturn(Optional.of(pedidoRecebido));

            mockMvc.perform(patch("/pedidos-compra/{id}/cancelar", PEDIDO_ID))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
