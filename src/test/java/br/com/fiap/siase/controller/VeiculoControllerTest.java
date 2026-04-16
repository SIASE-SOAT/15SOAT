package br.com.fiap.siase.controller;

import br.com.fiap.siase.config.SecurityConfig;
import br.com.fiap.siase.dto.response.VeiculoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.DuplicateResourceException;
import br.com.fiap.siase.exception.GlobalExceptionHandler;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.security.JwtService;
import br.com.fiap.siase.security.UserDetailsServiceImpl;
import br.com.fiap.siase.service.VeiculoService;
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

@WebMvcTest(VeiculoController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
@DisplayName("VeiculoController - Endpoints REST")
class VeiculoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private VeiculoService service;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private VeiculoResponse veiculoResponse;
    private UUID veiculoId;
    private UUID clienteId;

    @BeforeEach
    void setUp() {
        veiculoId = UUID.randomUUID();
        clienteId = UUID.randomUUID();
        veiculoResponse = new VeiculoResponse(
                veiculoId, "ABC1234", "Toyota", "Corolla", 2022,
                "Prata", true, clienteId, "João Silva", LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /veiculos")
    class ListarTodos {

        @Test
        @DisplayName("Deve retornar 200 com lista de veículos")
        void deveRetornar200() throws Exception {
            when(service.listarTodos()).thenReturn(List.of(veiculoResponse));

            mockMvc.perform(get("/veiculos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].placa", is("ABC1234")))
                    .andExpect(jsonPath("$[0].marca", is("Toyota")));
        }
    }

    @Nested
    @DisplayName("GET /veiculos/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar 200 com veículo")
        void deveRetornar200() throws Exception {
            when(service.buscarPorId(veiculoId)).thenReturn(veiculoResponse);

            mockMvc.perform(get("/veiculos/{id}", veiculoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.placa", is("ABC1234")))
                    .andExpect(jsonPath("$.clienteNome", is("João Silva")));
        }

        @Test
        @DisplayName("Deve retornar 404 para veículo inexistente")
        void deveRetornar404() throws Exception {
            when(service.buscarPorId(any())).thenThrow(new ResourceNotFoundException("Veículo", veiculoId));

            mockMvc.perform(get("/veiculos/{id}", veiculoId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /veiculos/placa/{placa}")
    class BuscarPorPlaca {

        @Test
        @DisplayName("Deve retornar 200 com veículo por placa")
        void deveRetornar200() throws Exception {
            when(service.buscarPorPlaca("ABC1234")).thenReturn(veiculoResponse);

            mockMvc.perform(get("/veiculos/placa/{placa}", "ABC1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modelo", is("Corolla")));
        }
    }

    @Nested
    @DisplayName("GET /veiculos/cliente/{clienteId}")
    class ListarPorCliente {

        @Test
        @DisplayName("Deve retornar lista de veículos do cliente")
        void deveRetornarLista() throws Exception {
            when(service.listarPorCliente(clienteId)).thenReturn(List.of(veiculoResponse));

            mockMvc.perform(get("/veiculos/cliente/{clienteId}", clienteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].clienteId", is(clienteId.toString())));
        }
    }

    @Nested
    @DisplayName("POST /veiculos")
    class Criar {

        private String validBody() {
            return """
                    {
                      "placa": "ABC1234",
                      "marca": "Toyota",
                      "modelo": "Corolla",
                      "ano": 2022,
                      "cor": "Prata",
                      "clienteId": "%s"
                    }
                    """.formatted(clienteId);
        }

        @Test
        @DisplayName("Deve retornar 201 ao criar veículo válido")
        void deveRetornar201() throws Exception {
            when(service.criar(any())).thenReturn(veiculoResponse);

            mockMvc.perform(post("/veiculos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.placa", is("ABC1234")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando placa não informada")
        void deveRetornar400QuandoPlacaAusente() throws Exception {
            String body = """
                    {
                      "marca": "Toyota",
                      "modelo": "Corolla",
                      "ano": 2022,
                      "clienteId": "%s"
                    }
                    """.formatted(clienteId);

            mockMvc.perform(post("/veiculos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 409 quando placa duplicada")
        void deveRetornar409QuandoPlacaDuplicada() throws Exception {
            when(service.criar(any())).thenThrow(new DuplicateResourceException("Já existe um veículo com a placa"));

            mockMvc.perform(post("/veiculos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /veiculos/{id}")
    class Atualizar {

        @Test
        @DisplayName("Deve retornar 200 ao atualizar veículo")
        void deveRetornar200() throws Exception {
            when(service.atualizar(eq(veiculoId), any())).thenReturn(veiculoResponse);

            mockMvc.perform(put("/veiculos/{id}", veiculoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "placa": "ABC1234",
                                      "marca": "Honda",
                                      "modelo": "Civic",
                                      "ano": 2023,
                                      "clienteId": "%s"
                                    }
                                    """.formatted(clienteId)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /veiculos/{id}")
    class Desativar {

        @Test
        @DisplayName("Deve retornar 204 ao desativar veículo")
        void deveRetornar204() throws Exception {
            doNothing().when(service).desativar(veiculoId);

            mockMvc.perform(delete("/veiculos/{id}", veiculoId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Deve retornar 404 ao desativar veículo inexistente")
        void deveRetornar404() throws Exception {
            doThrow(new ResourceNotFoundException("Veículo", veiculoId)).when(service).desativar(any());

            mockMvc.perform(delete("/veiculos/{id}", veiculoId))
                    .andExpect(status().isNotFound());
        }
    }
}
