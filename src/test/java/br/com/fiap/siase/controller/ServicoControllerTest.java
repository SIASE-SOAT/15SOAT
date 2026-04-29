package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.response.ServicoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.config.SecurityConfig;
import br.com.fiap.siase.exception.GlobalExceptionHandler;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.security.JwtService;
import br.com.fiap.siase.security.UserDetailsServiceImpl;
import br.com.fiap.siase.service.ServicoService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(ServicoController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
@DisplayName("ServicoController - Endpoints REST")
class ServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServicoService service;

    private ServicoResponse servicoResponse;
    private UUID servicoId;

    @BeforeEach
    void setUp() {
        servicoId = UUID.randomUUID();
        servicoResponse = new ServicoResponse(
                servicoId,
                "Troca de Óleo",
                "Substituição do óleo do motor",
                new BigDecimal("120.00"),
                60,
                true,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /servicos")
    class ListarAtivos {

        @Test
        @DisplayName("Deve retornar 200 com lista de serviços ativos")
        void deveRetornar200ComLista() throws Exception {
            when(service.listarAtivos()).thenReturn(List.of(servicoResponse));

            mockMvc.perform(get("/servicos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nome", is("Troca de Óleo")))
                    .andExpect(jsonPath("$[0].preco", is(120.00)))
                    .andExpect(jsonPath("$[0].ativo", is(true)));
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há serviços")
        void deveRetornarListaVazia() throws Exception {
            when(service.listarAtivos()).thenReturn(List.of());

            mockMvc.perform(get("/servicos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /servicos/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar 200 com o serviço encontrado")
        void deveRetornar200QuandoEncontrado() throws Exception {
            when(service.buscarPorId(servicoId)).thenReturn(servicoResponse);

            mockMvc.perform(get("/servicos/{id}", servicoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(servicoId.toString())))
                    .andExpect(jsonPath("$.nome", is("Troca de Óleo")));
        }

        @Test
        @DisplayName("Deve retornar 404 quando serviço não encontrado")
        void deveRetornar404QuandoNaoEncontrado() throws Exception {
            when(service.buscarPorId(any())).thenThrow(new ResourceNotFoundException("Serviço não encontrado: " + servicoId));

            mockMvc.perform(get("/servicos/{id}", servicoId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }
    }

    @Nested
    @DisplayName("POST /servicos")
    class Criar {

        @Test
        @DisplayName("Deve retornar 201 com Location e body ao criar")
        void deveRetornar201AoCriar() throws Exception {
            when(service.criar(any())).thenReturn(servicoResponse);

            String body = """
                    { "nome": "Troca de Óleo", "descricao": "Troca completa", "preco": 120.00 }
                    """;

            mockMvc.perform(post("/servicos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(servicoId.toString())))
                    .andExpect(jsonPath("$.nome", is("Troca de Óleo")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando nome está em branco")
        void deveRetornar400QuandoNomeEmBranco() throws Exception {
            String body = """
                    { "nome": "", "preco": 120.00 }
                    """;

            mockMvc.perform(post("/servicos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.nome", notNullValue()));
        }

        @Test
        @DisplayName("Deve retornar 400 quando preço é zero")
        void deveRetornar400QuandoPrecoZero() throws Exception {
            String body = """
                    { "nome": "Serviço", "preco": 0.00 }
                    """;

            mockMvc.perform(post("/servicos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.preco", notNullValue()));
        }

        @Test
        @DisplayName("Deve retornar 400 quando preço é nulo")
        void deveRetornar400QuandoPrecoNulo() throws Exception {
            String body = """
                    { "nome": "Serviço" }
                    """;

            mockMvc.perform(post("/servicos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.preco", notNullValue()));
        }
    }

    @Nested
    @DisplayName("PUT /servicos/{id}")
    class Atualizar {

        @Test
        @DisplayName("Deve retornar 200 ao atualizar com sucesso")
        void deveRetornar200AoAtualizar() throws Exception {
            when(service.atualizar(eq(servicoId), any())).thenReturn(servicoResponse);

            String body = """
                    { "nome": "Troca de Óleo Sintético", "preco": 150.00 }
                    """;

            mockMvc.perform(put("/servicos/{id}", servicoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve retornar 404 quando serviço não existe")
        void deveRetornar404QuandoNaoEncontrado() throws Exception {
            when(service.atualizar(any(), any())).thenThrow(new ResourceNotFoundException("Serviço não encontrado"));

            String body = """
                    { "nome": "X", "preco": 10.00 }
                    """;

            mockMvc.perform(put("/servicos/{id}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /servicos/{id}")
    class Desativar {

        @Test
        @DisplayName("Deve retornar 204 ao desativar com sucesso")
        void deveRetornar204AoDesativar() throws Exception {
            doNothing().when(service).desativar(servicoId);

            mockMvc.perform(delete("/servicos/{id}", servicoId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Deve retornar 404 ao tentar desativar serviço inexistente")
        void deveRetornar404QuandoNaoEncontrado() throws Exception {
            doThrow(new ResourceNotFoundException("Serviço não encontrado"))
                    .when(service).desativar(any());

            mockMvc.perform(delete("/servicos/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /servicos/{id}/insumos")
    class AdicionarInsumo {

        @Test
        @DisplayName("Deve retornar 200 ao vincular insumo com sucesso")
        void deveRetornar200AoAdicionarInsumo() throws Exception {
            when(service.adicionarInsumo(eq(servicoId), any())).thenReturn(servicoResponse);

            String body = String.format("""
                    { "pecaId": "%s", "quantidade": 4 }
                    """, UUID.randomUUID());

            mockMvc.perform(post("/servicos/{id}/insumos", servicoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve retornar 422 quando insumo já está vinculado")
        void deveRetornar422QuandoInsumoJaVinculado() throws Exception {
            when(service.adicionarInsumo(any(), any()))
                    .thenThrow(new BusinessException("Peça já está vinculada a este serviço."));

            String body = String.format("""
                    { "pecaId": "%s", "quantidade": 1 }
                    """, UUID.randomUUID());

            mockMvc.perform(post("/servicos/{id}/insumos", servicoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Deve retornar 400 quando quantidade é zero")
        void deveRetornar400QuandoQuantidadeZero() throws Exception {
            String body = String.format("""
                    { "pecaId": "%s", "quantidade": 0 }
                    """, UUID.randomUUID());

            mockMvc.perform(post("/servicos/{id}/insumos", servicoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.quantidade", notNullValue()));
        }
    }
}
