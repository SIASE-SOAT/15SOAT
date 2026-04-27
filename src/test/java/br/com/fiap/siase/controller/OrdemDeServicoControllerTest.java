package br.com.fiap.siase.controller;

import br.com.fiap.siase.config.SecurityConfig;
import br.com.fiap.siase.dto.response.OrdemDeServicoResponse;
import br.com.fiap.siase.dto.response.PreparacaoAberturaOrdemResponse;
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
    @DisplayName("POST /ordens/{id}/items-peca")
    class AdicionarPecaAOrdem {

        private UUID pecaId;

        @BeforeEach
        void setUp() {
            pecaId = UUID.randomUUID();
        }

        @Test
        @DisplayName("Deve retornar 200 ao adicionar peça com sucesso")
        void deveRetornar200AoAdicionarPeca() throws Exception {
            OrdemDeServicoResponse osComPeca = new OrdemDeServicoResponse(
                    osId, "OS-20260412-ABCDEF",
                    UUID.randomUUID(), "João",
                    UUID.randomUUID(), "ABC1234", "Corolla",
                    StatusOS.RECEBIDA.name(), StatusOS.RECEBIDA.getDescricao(),
                    null, List.of(),
                    List.of(new OrdemDeServicoResponse.ItemPecaResponse(
                            UUID.randomUUID(), pecaId, "OL001", "Filtro de Óleo",
                            2, new BigDecimal("50.00"), new BigDecimal("100.00")
                    )),
                    new BigDecimal("120.00"), new BigDecimal("100.00"), new BigDecimal("220.00"),
                    LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
            );
            when(service.adicionarPecaAOrdem(eq(osId), any())).thenReturn(osComPeca);

            String body = """
                    {
                      "pecaId": "%s",
                      "quantidade": 2
                    }
                    """.formatted(pecaId);

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itensPeca", hasSize(1)))
                    .andExpect(jsonPath("$.itensPeca[0].pecaNome", is("Filtro de Óleo")))
                    .andExpect(jsonPath("$.totalPecas", is(100.00)))
                    .andExpect(jsonPath("$.total", is(220.00)));
        }

        @Test
        @DisplayName("Deve retornar 400 quando pecaId está ausente")
        void deveRetornar400QuandoPecaIdAusente() throws Exception {
            String body = """
                    {
                      "quantidade": 2
                    }
                    """;

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.pecaId", notNullValue()));
        }

        @Test
        @DisplayName("Deve retornar 400 quando quantidade está ausente")
        void deveRetornar400QuandoQuantidadeAusente() throws Exception {
            String body = """
                    {
                      "pecaId": "%s"
                    }
                    """.formatted(pecaId);

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.quantidade", notNullValue()));
        }

        @Test
        @DisplayName("Deve retornar 400 quando quantidade é menor que 1")
        void deveRetornar400QuandoQuantidadeMenorQue1() throws Exception {
            String body = """
                    {
                      "pecaId": "%s",
                      "quantidade": 0
                    }
                    """.formatted(pecaId);

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.quantidade", notNullValue()));
        }

        @Test
        @DisplayName("Deve retornar 404 quando OS não existe")
        void deveRetornar404QuandoOSNaoExiste() throws Exception {
            when(service.adicionarPecaAOrdem(any(), any()))
                    .thenThrow(new ResourceNotFoundException("OS não encontrada"));

            String body = """
                    {
                      "pecaId": "%s",
                      "quantidade": 2
                    }
                    """.formatted(pecaId);

            mockMvc.perform(post("/ordens/{id}/items-peca", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 404 quando peça não existe")
        void deveRetornar404QuandoPecaNaoExiste() throws Exception {
            when(service.adicionarPecaAOrdem(any(), any()))
                    .thenThrow(new ResourceNotFoundException("Peça não encontrada"));

            String body = """
                    {
                      "pecaId": "%s",
                      "quantidade": 2
                    }
                    """.formatted(pecaId);

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 422 quando estoque insuficiente")
        void deveRetornar422QuandoEstoqueInsuficiente() throws Exception {
            when(service.adicionarPecaAOrdem(any(), any()))
                    .thenThrow(new BusinessException("Estoque insuficiente para a peça: Filtro de Óleo. Disponível: 1"));

            String body = """
                    {
                      "pecaId": "%s",
                      "quantidade": 5
                    }
                    """.formatted(pecaId);

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("Estoque insuficiente")));
        }

        @Test
        @DisplayName("Deve retornar 422 quando peça já foi adicionada")
        void deveRetornar422QuandoPecaJaAdicionada() throws Exception {
            when(service.adicionarPecaAOrdem(any(), any()))
                    .thenThrow(new BusinessException("Esta peça já foi adicionada a esta ordem de serviço."));

            String body = """
                    {
                      "pecaId": "%s",
                      "quantidade": 2
                    }
                    """.formatted(pecaId);

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("já foi adicionada")));
        }

        @Test
        @DisplayName("Deve retornar 422 quando status não permite adicionar peça")
        void deveRetornar422QuandoStatusNaoPermite() throws Exception {
            when(service.adicionarPecaAOrdem(any(), any()))
                    .thenThrow(new BusinessException("Não é possível adicionar peças em uma ordem com status Entregue"));

            String body = """
                    {
                      "pecaId": "%s",
                      "quantidade": 2
                    }
                    """.formatted(pecaId);

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("status")));
        }

        @Test
        @DisplayName("Deve retornar 422 quando peça está desativada")
        void deveRetornar422QuandoPecaDesativada() throws Exception {
            when(service.adicionarPecaAOrdem(any(), any()))
                    .thenThrow(new BusinessException("Peça desativada não pode ser adicionada"));

            String body = """
                    {
                      "pecaId": "%s",
                      "quantidade": 2
                    }
                    """.formatted(pecaId);

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("desativada")));
        }
    }

    @Nested
    @DisplayName("POST /ordens/{id}/items-servico")
    class AdicionarServicoAOrdem {

        private UUID servicoId;

        @BeforeEach
        void setUp() {
            servicoId = UUID.randomUUID();
        }

        @Test
        @DisplayName("Deve retornar 200 ao adicionar serviço com sucesso")
        void deveRetornar200AoAdicionarServico() throws Exception {
            OrdemDeServicoResponse osComServico = new OrdemDeServicoResponse(
                    osId, "OS-20260412-ABCDEF",
                    UUID.randomUUID(), "João",
                    UUID.randomUUID(), "ABC1234", "Corolla",
                    StatusOS.RECEBIDA.name(), StatusOS.RECEBIDA.getDescricao(),
                    null,
                    List.of(new OrdemDeServicoResponse.ItemServicoResponse(
                            UUID.randomUUID(), servicoId, "Revisão Completa",
                            new BigDecimal("150.00"), 120, null, null, null
                    )),
                    List.of(),
                    new BigDecimal("150.00"), BigDecimal.ZERO, new BigDecimal("150.00"),
                    LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
            );
            when(service.adicionarServicoAOrdem(eq(osId), any())).thenReturn(osComServico);

            String body = """
                    {
                      "servicoId": "%s"
                    }
                    """.formatted(servicoId);

            mockMvc.perform(post("/ordens/{id}/items-servico", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itensServico", hasSize(1)))
                    .andExpect(jsonPath("$.itensServico[0].servicoNome", is("Revisão Completa")))
                    .andExpect(jsonPath("$.totalServicos", is(150.00)))
                    .andExpect(jsonPath("$.total", is(150.00)));
        }

        @Test
        @DisplayName("Deve retornar 400 quando servicoId está ausente")
        void deveRetornar400QuandoServicoIdAusente() throws Exception {
            String body = "{}";

            mockMvc.perform(post("/ordens/{id}/items-servico", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.servicoId", notNullValue()));
        }

        @Test
        @DisplayName("Deve retornar 404 quando OS não existe")
        void deveRetornar404QuandoOSNaoExiste() throws Exception {
            when(service.adicionarServicoAOrdem(any(), any()))
                    .thenThrow(new ResourceNotFoundException("OS não encontrada"));

            String body = """
                    {
                      "servicoId": "%s"
                    }
                    """.formatted(servicoId);

            mockMvc.perform(post("/ordens/{id}/items-servico", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 404 quando serviço não existe")
        void deveRetornar404QuandoServicoNaoExiste() throws Exception {
            when(service.adicionarServicoAOrdem(any(), any()))
                    .thenThrow(new ResourceNotFoundException("Serviço não encontrado"));

            String body = """
                    {
                      "servicoId": "%s"
                    }
                    """.formatted(servicoId);

            mockMvc.perform(post("/ordens/{id}/items-servico", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 422 quando serviço já foi adicionado")
        void deveRetornar422QuandoServicoJaAdicionado() throws Exception {
            when(service.adicionarServicoAOrdem(any(), any()))
                    .thenThrow(new BusinessException("Este serviço já foi adicionado a esta ordem de serviço."));

            String body = """
                    {
                      "servicoId": "%s"
                    }
                    """.formatted(servicoId);

            mockMvc.perform(post("/ordens/{id}/items-servico", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("já foi adicionado")));
        }

        @Test
        @DisplayName("Deve retornar 422 quando status não permite adicionar serviço")
        void deveRetornar422QuandoStatusNaoPermite() throws Exception {
            when(service.adicionarServicoAOrdem(any(), any()))
                    .thenThrow(new BusinessException("Não é possível adicionar serviços em uma ordem com status Entregue"));

            String body = """
                    {
                      "servicoId": "%s"
                    }
                    """.formatted(servicoId);

            mockMvc.perform(post("/ordens/{id}/items-servico", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("status")));
        }

        @Test
        @DisplayName("Deve retornar 422 quando serviço está desativado")
        void deveRetornar422QuandoServicoDesativado() throws Exception {
            when(service.adicionarServicoAOrdem(any(), any()))
                    .thenThrow(new BusinessException("Serviço desativado não pode ser adicionado"));

            String body = """
                    {
                      "servicoId": "%s"
                    }
                    """.formatted(servicoId);

            mockMvc.perform(post("/ordens/{id}/items-servico", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("desativado")));
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
    @DisplayName("GET /ordens/preparar-abertura")
    class PrepararAbertura {

        private PreparacaoAberturaOrdemResponse response;

        @BeforeEach
        void setUpPreparacao() {
            response = new PreparacaoAberturaOrdemResponse(
                    new PreparacaoAberturaOrdemResponse.ClienteIdentificadoResponse(
                            UUID.randomUUID(),
                            "João da Silva",
                            "52998224725",
                            "joao@email.com",
                            "11999999999"
                    ),
                    List.of(new PreparacaoAberturaOrdemResponse.VeiculoIdentificadoResponse(
                            UUID.randomUUID(),
                            "ABC1234",
                            "Toyota",
                            "Corolla",
                            2022,
                            true
                    )),
                    null,
                    false
            );
        }

        @Test
        @DisplayName("Deve retornar 200 ao preparar abertura apenas com documento")
        void deveRetornar200ComDocumento() throws Exception {
            when(service.prepararAbertura("52998224725", null)).thenReturn(response);

            mockMvc.perform(get("/ordens/preparar-abertura")
                            .param("documento", "52998224725"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cliente.nome", is("João da Silva")))
                    .andExpect(jsonPath("$.veiculos", hasSize(1)))
                    .andExpect(jsonPath("$.veiculos[0].placa", is("ABC1234")))
                    .andExpect(jsonPath("$.prontoParaAbertura", is(false)));
        }

        @Test
        @DisplayName("Deve retornar 200 ao preparar abertura com documento e placa")
        void deveRetornar200ComDocumentoEPlaca() throws Exception {
            PreparacaoAberturaOrdemResponse comVeiculoSelecionado = new PreparacaoAberturaOrdemResponse(
                    response.cliente(),
                    response.veiculos(),
                    response.veiculos().get(0),
                    true
            );
            when(service.prepararAbertura("52998224725", "ABC1234")).thenReturn(comVeiculoSelecionado);

            mockMvc.perform(get("/ordens/preparar-abertura")
                            .param("documento", "52998224725")
                            .param("placa", "ABC1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.veiculoSelecionado.placa", is("ABC1234")))
                    .andExpect(jsonPath("$.prontoParaAbertura", is(true)));
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
