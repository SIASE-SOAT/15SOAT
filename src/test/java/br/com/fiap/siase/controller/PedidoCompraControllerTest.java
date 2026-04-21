package br.com.fiap.siase.controller;

import br.com.fiap.siase.config.SecurityConfig;
import br.com.fiap.siase.dto.response.PedidoCompraResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.GlobalExceptionHandler;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.enums.StatusPedidoCompra;
import br.com.fiap.siase.security.JwtService;
import br.com.fiap.siase.security.UserDetailsServiceImpl;
import br.com.fiap.siase.service.PedidoCompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoCompraController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
@DisplayName("PedidoCompraController - Endpoints REST")
class PedidoCompraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoCompraService service;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private UUID pedidoId;
    private UUID pecaId;
    private PedidoCompraResponse response;

    @BeforeEach
    void setUp() {
        pedidoId = UUID.randomUUID();
        pecaId = UUID.randomUUID();
        response = new PedidoCompraResponse(
                pedidoId,
                pecaId,
                "PEC-001",
                "Filtro de Oleo",
                5,
                0,
            StatusPedidoCompra.PENDENTE.name(),
            StatusPedidoCompra.PENDENTE.getDescricao(),
                "Compra urgente",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /pedidos-compra")
    class Criar {

        @Test
        @DisplayName("Deve retornar 201 ao criar pedido")
        void deveCriarPedido() throws Exception {
            when(service.criar(any())).thenReturn(response);

            mockMvc.perform(post("/pedidos-compra")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "pecaId": "%s",
                                      "quantidadeSolicitada": 5,
                                      "observacoes": "Compra urgente"
                                    }
                                    """.formatted(pecaId)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(pedidoId.toString())))
                    .andExpect(jsonPath("$.pecaCodigo", is("PEC-001")));
        }

        @Test
        @DisplayName("Deve retornar 400 para payload inválido")
        void deveRetornar400ParaPayloadInvalido() throws Exception {
            mockMvc.perform(post("/pedidos-compra")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "quantidadeSolicitada": 0
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET e PATCH /pedidos-compra")
    class LeituraETransicoes {

        @Test
        @DisplayName("Deve listar todos sem filtro")
        void deveListarTodos() throws Exception {
            when(service.listar()).thenReturn(List.of(response));

            mockMvc.perform(get("/pedidos-compra"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("PENDENTE")));

            verify(service).listar();
            verify(service, never()).listarPorStatus(any());
        }

        @Test
        @DisplayName("Deve listar por status quando filtro existir")
        void deveListarPorStatus() throws Exception {
            when(service.listarPorStatus(StatusPedidoCompra.PENDENTE)).thenReturn(List.of(response));

            mockMvc.perform(get("/pedidos-compra").param("status", "PENDENTE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("Deve buscar pedido por id")
        void deveBuscarPorId() throws Exception {
            when(service.buscarPorId(pedidoId)).thenReturn(response);

            mockMvc.perform(get("/pedidos-compra/{id}", pedidoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(pedidoId.toString())));
        }

        @Test
        @DisplayName("Deve aprovar pedido")
        void deveAprovarPedido() throws Exception {
            PedidoCompraResponse aprovado = new PedidoCompraResponse(
                    pedidoId,
                    pecaId,
                    "PEC-001",
                    "Filtro de Oleo",
                    5,
                    0,
                    "APROVADO",
                    "Aprovado",
                    "Compra urgente",
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            when(service.aprovar(pedidoId)).thenReturn(aprovado);

            mockMvc.perform(patch("/pedidos-compra/{id}/aprovar", pedidoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("APROVADO")));
        }

        @Test
        @DisplayName("Deve receber pedido com quantidade válida")
        void deveReceberPedido() throws Exception {
            PedidoCompraResponse recebido = new PedidoCompraResponse(
                    pedidoId,
                    pecaId,
                    "PEC-001",
                    "Filtro de Oleo",
                    5,
                    5,
                    "RECEBIDO",
                    "Recebido",
                    "Compra urgente",
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            when(service.receber(pedidoId, 5)).thenReturn(recebido);

            mockMvc.perform(patch("/pedidos-compra/{id}/receber", pedidoId).param("quantidade", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantidadeRecebida", is(5)));
        }

        @Test
        @DisplayName("Deve retornar 400 para quantidade inválida no recebimento")
        void deveRetornar400ParaQuantidadeInvalida() throws Exception {
            mockMvc.perform(patch("/pedidos-compra/{id}/receber", pedidoId).param("quantidade", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve cancelar pedido")
        void deveCancelarPedido() throws Exception {
            PedidoCompraResponse cancelado = new PedidoCompraResponse(
                    pedidoId,
                    pecaId,
                    "PEC-001",
                    "Filtro de Oleo",
                    5,
                    0,
                    "CANCELADO",
                    "Cancelado",
                    "Compra urgente",
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            when(service.cancelar(pedidoId)).thenReturn(cancelado);

            mockMvc.perform(patch("/pedidos-compra/{id}/cancelar", pedidoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELADO")));
        }

        @Test
        @DisplayName("Deve retornar 404 quando pedido não existe")
        void deveRetornar404QuandoNaoExiste() throws Exception {
            when(service.buscarPorId(pedidoId)).thenThrow(new ResourceNotFoundException("Pedido não encontrado"));

            mockMvc.perform(get("/pedidos-compra/{id}", pedidoId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 422 em regra de negócio")
        void deveRetornar422EmRegraDeNegocio() throws Exception {
            when(service.aprovar(pedidoId)).thenThrow(new BusinessException("Pedido já aprovado."));

            mockMvc.perform(patch("/pedidos-compra/{id}/aprovar", pedidoId))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("aprovado")));
        }
    }
}
