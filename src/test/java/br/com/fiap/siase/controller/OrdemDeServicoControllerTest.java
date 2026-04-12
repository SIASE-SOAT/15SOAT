package br.com.fiap.siase.controller;

import br.com.fiap.siase.config.SecurityConfig;
import br.com.fiap.siase.dto.response.OrdemDeServicoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.GlobalExceptionHandler;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.enums.StatusOS;
import br.com.fiap.siase.security.JwtService;
import br.com.fiap.siase.security.UserDetailsServiceImpl;
import br.com.fiap.siase.service.OrdemDeServicoService;
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

@WebMvcTest(OrdemDeServicoController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
@DisplayName("OrdemDeServicoController - Endpoints REST")
class OrdemDeServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private OrdemDeServicoService service;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private OrdemDeServicoResponse osResponse;
    private UUID osId;

    @BeforeEach
    void setUp() {
        osId = UUID.randomUUID();
        osResponse = new OrdemDeServicoResponse(
                osId,
                "OS-20260412-ABCDEF",
                UUID.randomUUID(), "João da Silva",
                UUID.randomUUID(), "ABC1234", "Corolla",
                StatusOS.RECEBIDA.name(), StatusOS.RECEBIDA.getDescricao(),
                "Barulho ao frear",
                List.of(), List.of(),
                new BigDecimal("120.00"), BigDecimal.ZERO, new BigDecimal("120.00"),
                LocalDateTime.now(), null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /ordens")
    class CriarOS {

        @Test
        @DisplayName("Deve retornar 201 com Location e body ao criar OS")
        void deveRetornar201AoCriar() throws Exception {
            when(service.criar(any())).thenReturn(osResponse);

            String body = """
                    {
                      "clienteId": "%s",
                      "veiculoId": "%s",
                      "observacoes": "Barulho ao frear",
                      "itensServico": [
                        { "servicoId": "%s" }
                      ]
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(post("/ordens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(osId.toString())))
                    .andExpect(jsonPath("$.numero", is("OS-20260412-ABCDEF")))
                    .andExpect(jsonPath("$.status", is("RECEBIDA")))
                    .andExpect(jsonPath("$.total", is(120.00)));
        }

        @Test
        @DisplayName("Deve retornar 400 quando clienteId está ausente")
        void deveRetornar400QuandoClienteIdAusente() throws Exception {
            String body = """
                    {
                      "veiculoId": "%s",
                      "itensServico": [{ "servicoId": "%s" }]
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(post("/ordens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.clienteId", notNullValue()));
        }

        @Test
        @DisplayName("Deve retornar 400 quando veiculoId está ausente")
        void deveRetornar400QuandoVeiculoIdAusente() throws Exception {
            String body = """
                    {
                      "clienteId": "%s",
                      "itensServico": [{ "servicoId": "%s" }]
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(post("/ordens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.veiculoId", notNullValue()));
        }

        @Test
        @DisplayName("Deve retornar 400 quando itensServico está vazio")
        void deveRetornar400QuandoItensServicoVazio() throws Exception {
            String body = """
                    {
                      "clienteId": "%s",
                      "veiculoId": "%s",
                      "itensServico": []
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(post("/ordens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.itensServico", notNullValue()));
        }

        @Test
        @DisplayName("Deve retornar 400 quando servicoId dentro do item está ausente")
        void deveRetornar400QuandoServicoIdAusente() throws Exception {
            String body = """
                    {
                      "clienteId": "%s",
                      "veiculoId": "%s",
                      "itensServico": [{}]
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(post("/ordens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 404 quando cliente não existe")
        void deveRetornar404QuandoClienteNaoExiste() throws Exception {
            when(service.criar(any()))
                    .thenThrow(new ResourceNotFoundException("Cliente não encontrado"));

            String body = """
                    {
                      "clienteId": "%s",
                      "veiculoId": "%s",
                      "itensServico": [{ "servicoId": "%s" }]
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(post("/ordens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }

        @Test
        @DisplayName("Deve retornar 422 quando veículo não pertence ao cliente")
        void deveRetornar422QuandoVeiculoDeOutroCliente() throws Exception {
            when(service.criar(any()))
                    .thenThrow(new BusinessException("O veículo informado não pertence ao cliente."));

            String body = """
                    {
                      "clienteId": "%s",
                      "veiculoId": "%s",
                      "itensServico": [{ "servicoId": "%s" }]
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(post("/ordens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("não pertence ao cliente")));
        }

        @Test
        @DisplayName("Deve retornar 422 quando estoque insuficiente para a peça")
        void deveRetornar422QuandoEstoqueInsuficiente() throws Exception {
            when(service.criar(any()))
                    .thenThrow(new BusinessException("Estoque insuficiente para a peça: Filtro de Óleo. Disponível: 2"));

            String body = """
                    {
                      "clienteId": "%s",
                      "veiculoId": "%s",
                      "itensServico": [{ "servicoId": "%s" }],
                      "itensPeca": [{ "pecaId": "%s", "quantidade": 10 }]
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(post("/ordens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("Estoque insuficiente")));
        }
    }

    @Nested
    @DisplayName("GET /ordens")
    class ListarOS {

        @Test
        @DisplayName("Deve retornar 200 com lista de todas as OS")
        void deveRetornar200ComLista() throws Exception {
            when(service.listar()).thenReturn(List.of(osResponse));

            mockMvc.perform(get("/ordens"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].numero", is("OS-20260412-ABCDEF")))
                    .andExpect(jsonPath("$[0].status", is("RECEBIDA")));
        }

        @Test
        @DisplayName("Deve retornar 200 com lista filtrada por status")
        void deveRetornar200ComListaFiltradaPorStatus() throws Exception {
            OrdemDeServicoResponse osEmExecucao = new OrdemDeServicoResponse(
                    UUID.randomUUID(), "OS-20260412-XYZ789",
                    UUID.randomUUID(), "Maria",
                    UUID.randomUUID(), "BRA2E19", "Civic",
                    StatusOS.EM_EXECUCAO.name(), StatusOS.EM_EXECUCAO.getDescricao(),
                    null, List.of(), List.of(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
            );
            when(service.listarPorStatus(StatusOS.EM_EXECUCAO)).thenReturn(List.of(osEmExecucao));

            mockMvc.perform(get("/ordens").param("status", "EM_EXECUCAO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("EM_EXECUCAO")));

            verify(service).listarPorStatus(StatusOS.EM_EXECUCAO);
            verify(service, never()).listar();
        }

        @Test
        @DisplayName("Deve retornar 200 com lista vazia")
        void deveRetornar200ComListaVazia() throws Exception {
            when(service.listar()).thenReturn(List.of());

            mockMvc.perform(get("/ordens"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /ordens/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar 200 com a OS encontrada")
        void deveRetornar200QuandoEncontrada() throws Exception {
            when(service.buscarPorId(osId)).thenReturn(osResponse);

            mockMvc.perform(get("/ordens/{id}", osId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(osId.toString())))
                    .andExpect(jsonPath("$.numero", is("OS-20260412-ABCDEF")))
                    .andExpect(jsonPath("$.statusDescricao", is("Recebida")));
        }

        @Test
        @DisplayName("Deve retornar 404 quando OS não encontrada")
        void deveRetornar404QuandoNaoEncontrada() throws Exception {
            when(service.buscarPorId(any()))
                    .thenThrow(new ResourceNotFoundException("OS não encontrada"));

            mockMvc.perform(get("/ordens/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }
    }

    @Nested
    @DisplayName("PATCH /ordens/{id}/avancar")
    class AvancarStatus {

        @Test
        @DisplayName("Deve retornar 200 ao avançar status com sucesso")
        void deveRetornar200AoAvancar() throws Exception {
            OrdemDeServicoResponse avancada = new OrdemDeServicoResponse(
                    osId, "OS-20260412-ABCDEF",
                    UUID.randomUUID(), "João",
                    UUID.randomUUID(), "ABC1234", "Corolla",
                    StatusOS.EM_DIAGNOSTICO.name(), StatusOS.EM_DIAGNOSTICO.getDescricao(),
                    null, List.of(), List.of(),
                    new BigDecimal("120.00"), BigDecimal.ZERO, new BigDecimal("120.00"),
                    LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
            );
            when(service.avancarStatus(osId)).thenReturn(avancada);

            mockMvc.perform(patch("/ordens/{id}/avancar", osId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("EM_DIAGNOSTICO")));
        }

        @Test
        @DisplayName("Deve retornar 404 quando OS não existe")
        void deveRetornar404QuandoNaoEncontrada() throws Exception {
            when(service.avancarStatus(any()))
                    .thenThrow(new ResourceNotFoundException("OS não encontrada"));

            mockMvc.perform(patch("/ordens/{id}/avancar", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 422 ao tentar avançar OS já entregue")
        void deveRetornar422QuandoOSJaEntregue() throws Exception {
            when(service.avancarStatus(any()))
                    .thenThrow(new BusinessException("Ordem de serviço já foi entregue."));

            mockMvc.perform(patch("/ordens/{id}/avancar", UUID.randomUUID()))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("já foi entregue")));
        }
    }

    @Nested
    @DisplayName("GET /ordens/acompanhar/{numero} (público)")
    class AcompanharOS {

        @Test
        @DisplayName("Deve retornar 200 sem autenticação pelo número da OS")
        @WithMockUser  // override necessário para endpoint público
        void deveRetornar200SemAutenticacao() throws Exception {
            when(service.buscarPorNumero("OS-20260412-ABCDEF")).thenReturn(osResponse);

            mockMvc.perform(get("/ordens/acompanhar/OS-20260412-ABCDEF"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.numero", is("OS-20260412-ABCDEF")))
                    .andExpect(jsonPath("$.status", is("RECEBIDA")))
                    .andExpect(jsonPath("$.statusDescricao", is("Recebida")));
        }

        @Test
        @DisplayName("Deve retornar 404 quando número da OS não existe")
        void deveRetornar404QuandoNumeroNaoExiste() throws Exception {
            when(service.buscarPorNumero(any()))
                    .thenThrow(new ResourceNotFoundException("OS não encontrada"));

            mockMvc.perform(get("/ordens/acompanhar/OS-INEXISTENTE"))
                    .andExpect(status().isNotFound());
        }
    }
}
