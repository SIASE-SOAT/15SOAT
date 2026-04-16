package br.com.fiap.siase.controller;

import br.com.fiap.siase.config.SecurityConfig;
import br.com.fiap.siase.dto.response.ClienteResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.DuplicateResourceException;
import br.com.fiap.siase.exception.GlobalExceptionHandler;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.security.JwtService;
import br.com.fiap.siase.security.UserDetailsServiceImpl;
import br.com.fiap.siase.service.ClienteService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
@DisplayName("ClienteController - Endpoints REST")
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private ClienteService service;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private ClienteResponse clienteResponse;
    private UUID clienteId;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
        clienteResponse = new ClienteResponse(
                clienteId, "João Silva", "PF", "52998224725",
                "joao@email.com", "11999999999", "Rua A, 100",
                true, LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /clientes")
    class ListarTodos {

        @Test
        @DisplayName("Deve retornar 200 com lista de clientes")
        void deveRetornar200ComLista() throws Exception {
            when(service.listarTodos()).thenReturn(List.of(clienteResponse));

            mockMvc.perform(get("/clientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nome", is("João Silva")))
                    .andExpect(jsonPath("$[0].tipoPessoa", is("PF")));
        }

        @Test
        @DisplayName("Deve retornar lista vazia")
        void deveRetornarListaVazia() throws Exception {
            when(service.listarTodos()).thenReturn(List.of());

            mockMvc.perform(get("/clientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /clientes/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar 200 com cliente encontrado")
        void deveRetornar200() throws Exception {
            when(service.buscarPorId(clienteId)).thenReturn(clienteResponse);

            mockMvc.perform(get("/clientes/{id}", clienteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(clienteId.toString())))
                    .andExpect(jsonPath("$.documento", is("52998224725")));
        }

        @Test
        @DisplayName("Deve retornar 404 para cliente inexistente")
        void deveRetornar404() throws Exception {
            when(service.buscarPorId(any())).thenThrow(new ResourceNotFoundException("Cliente", clienteId));

            mockMvc.perform(get("/clientes/{id}", clienteId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /clientes/documento/{documento}")
    class BuscarPorDocumento {

        @Test
        @DisplayName("Deve retornar 200 com cliente por documento")
        void deveRetornar200() throws Exception {
            when(service.buscarPorDocumento("52998224725")).thenReturn(clienteResponse);

            mockMvc.perform(get("/clientes/documento/{doc}", "52998224725"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome", is("João Silva")));
        }
    }

    @Nested
    @DisplayName("POST /clientes")
    class Criar {

        private String validBody() {
            return """
                    {
                      "nome": "João Silva",
                      "tipoPessoa": "PF",
                      "documento": "529.982.247-25",
                      "email": "joao@email.com",
                      "telefone": "11999999999",
                      "endereco": "Rua A, 100"
                    }
                    """;
        }

        @Test
        @DisplayName("Deve retornar 201 ao criar cliente válido")
        void deveRetornar201() throws Exception {
            when(service.criar(any())).thenReturn(clienteResponse);

            mockMvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome", is("João Silva")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando nome não informado")
        void deveRetornar400QuandoNomeAusente() throws Exception {
            String body = """
                    {
                      "tipoPessoa": "PF",
                      "documento": "529.982.247-25"
                    }
                    """;

            mockMvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 409 quando documento duplicado")
        void deveRetornar409QuandoDuplicado() throws Exception {
            when(service.criar(any())).thenThrow(new DuplicateResourceException("Já existe um cliente com o documento"));

            mockMvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /clientes/{id}")
    class Atualizar {

        @Test
        @DisplayName("Deve retornar 200 ao atualizar cliente")
        void deveRetornar200() throws Exception {
            when(service.atualizar(eq(clienteId), any())).thenReturn(clienteResponse);

            mockMvc.perform(put("/clientes/{id}", clienteId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nome": "João Atualizado",
                                      "tipoPessoa": "PF",
                                      "documento": "529.982.247-25"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome", is("João Silva")));
        }
    }

    @Nested
    @DisplayName("DELETE /clientes/{id}")
    class Desativar {

        @Test
        @DisplayName("Deve retornar 204 ao desativar cliente")
        void deveRetornar204() throws Exception {
            doNothing().when(service).desativar(clienteId);

            mockMvc.perform(delete("/clientes/{id}", clienteId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Deve retornar 404 ao desativar cliente inexistente")
        void deveRetornar404() throws Exception {
            doThrow(new ResourceNotFoundException("Cliente", clienteId)).when(service).desativar(any());

            mockMvc.perform(delete("/clientes/{id}", clienteId))
                    .andExpect(status().isNotFound());
        }
    }
}
