package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.response.PecaResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.config.SecurityConfig;
import br.com.fiap.siase.exception.GlobalExceptionHandler;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.security.JwtService;
import br.com.fiap.siase.security.UserDetailsServiceImpl;
import br.com.fiap.siase.service.PecaService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PecaController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
@DisplayName("PecaController - Endpoints REST")
class PecaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private PecaService service;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private PecaResponse pecaResponse;
    private UUID pecaId;

    @BeforeEach
    void setUp() {
        pecaId = UUID.randomUUID();
        pecaResponse = new PecaResponse(
                pecaId, "FILTRO-001", "Filtro de Óleo", "Filtro original",
                new BigDecimal("35.00"), 50, 10, "UN", true, false,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /pecas")
    class ListarAtivas {

        @Test
        @DisplayName("Deve retornar 200 com lista de peças ativas")
        void deveRetornar200ComLista() throws Exception {
            when(service.listarAtivas()).thenReturn(List.of(pecaResponse));

            mockMvc.perform(get("/pecas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].codigo", is("FILTRO-001")))
                    .andExpect(jsonPath("$[0].quantidadeEstoque", is(50)))
                    .andExpect(jsonPath("$[0].estoqueAbaixoMinimo", is(false)));
        }
    }

    @Nested
    @DisplayName("POST /pecas")
    class Criar {

        @Test
        @DisplayName("Deve retornar 201 com Location ao criar peça")
        void deveRetornar201AoCriar() throws Exception {
            when(service.criar(any())).thenReturn(pecaResponse);

            String body = """
                    {
                        "codigo": "FILTRO-001",
                        "nome": "Filtro de Óleo",
                        "preco": 35.00,
                        "quantidadeEstoque": 50,
                        "estoqueMinimo": 10,
                        "unidadeMedida": "UN"
                    }
                    """;

            mockMvc.perform(post("/pecas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(pecaId.toString())))
                    .andExpect(jsonPath("$.codigo", is("FILTRO-001")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando código está em branco")
        void deveRetornar400QuandoCodigoEmBranco() throws Exception {
            String body = """
                    { "codigo": "", "nome": "Filtro", "preco": 35.00 }
                    """;

            mockMvc.perform(post("/pecas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.codigo", notNullValue()));
        }

        @Test
        @DisplayName("Deve retornar 400 quando preço é negativo")
        void deveRetornar400QuandoPrecoNegativo() throws Exception {
            String body = """
                    { "codigo": "X-001", "nome": "Peça", "preco": -10.00 }
                    """;

            mockMvc.perform(post("/pecas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.preco", notNullValue()));
        }
    }

    @Nested
    @DisplayName("PATCH /pecas/{id}/estoque")
    class MovimentarEstoque {

        @Test
        @DisplayName("Deve retornar 200 ao registrar entrada de estoque")
        void deveRetornar200EmEntrada() throws Exception {
            PecaResponse atualizado = new PecaResponse(
                    pecaId, "FILTRO-001", "Filtro de Óleo", null,
                    new BigDecimal("35.00"), 70, 10, "UN", true, false,
                    LocalDateTime.now(), LocalDateTime.now()
            );
            when(service.movimentarEstoque(eq(pecaId), any())).thenReturn(atualizado);

            String body = """
                    { "operacao": "ENTRADA", "quantidade": 20 }
                    """;

            mockMvc.perform(patch("/pecas/{id}/estoque", pecaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantidadeEstoque", is(70)));
        }

        @Test
        @DisplayName("Deve retornar 422 ao registrar saída com estoque insuficiente")
        void deveRetornar422EmSaidaInsuficiente() throws Exception {
            when(service.movimentarEstoque(any(), any()))
                    .thenThrow(new BusinessException("Estoque insuficiente. Disponível: 5, solicitado: 10"));

            String body = """
                    { "operacao": "SAIDA", "quantidade": 10 }
                    """;

            mockMvc.perform(patch("/pecas/{id}/estoque", pecaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("Estoque insuficiente")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando quantidade é zero")
        void deveRetornar400QuandoQuantidadeZero() throws Exception {
            String body = """
                    { "operacao": "ENTRADA", "quantidade": 0 }
                    """;

            mockMvc.perform(patch("/pecas/{id}/estoque", pecaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 404 quando peça não existe")
        void deveRetornar404QuandoPecaNaoExiste() throws Exception {
            when(service.movimentarEstoque(any(), any()))
                    .thenThrow(new ResourceNotFoundException("Peça não encontrada"));

            String body = """
                    { "operacao": "ENTRADA", "quantidade": 10 }
                    """;

            mockMvc.perform(patch("/pecas/{id}/estoque", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /pecas/{id}")
    class Desativar {

        @Test
        @DisplayName("Deve retornar 204 ao desativar com sucesso")
        void deveRetornar204() throws Exception {
            doNothing().when(service).desativar(pecaId);

            mockMvc.perform(delete("/pecas/{id}", pecaId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Deve retornar 404 quando peça não existe")
        void deveRetornar404QuandoNaoEncontrada() throws Exception {
            doThrow(new ResourceNotFoundException("Peça não encontrada"))
                    .when(service).desativar(any());

            mockMvc.perform(delete("/pecas/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }
}
