package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.MovimentacaoEstoqueRequest;
import br.com.fiap.siase.application.dto.input.PecaRequest;
import br.com.fiap.siase.domain.model.Peca;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
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
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PecaController - Endpoints REST")
class PecaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PecaRepositoryPort pecaRepository;

    private static final UUID PECA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private Peca pecaSalva;

    @BeforeEach
    void setUp() {
        pecaSalva = new Peca();
        pecaSalva.setId(PECA_ID);
        pecaSalva.setCodigo("FILTRO-01");
        pecaSalva.setNome("Filtro de Óleo");
        pecaSalva.setDescricao("Filtro sintético");
        pecaSalva.setPreco(new BigDecimal("45.90"));
        pecaSalva.setQuantidadeEstoque(10);
        pecaSalva.setEstoqueMinimo(2);
        pecaSalva.setUnidadeMedida("UN");
        pecaSalva.setAtivo(true);
        pecaSalva.setCriadoEm(LocalDateTime.of(2024, 3, 15, 10, 0));
    }

    private PecaRequest requestValido() {
        return new PecaRequest("FILTRO-01", "Filtro de Óleo", "Filtro sintético",
                new BigDecimal("45.90"), 10, 2, "UN");
    }

    @Nested
    @DisplayName("GET /pecas - Listar peças ativas")
    class ListarAtivas {

        @Test
        @DisplayName("deve retornar 200 com lista de peças ativas")
        void deveListarPecasAtivas() throws Exception {
            when(pecaRepository.findByAtivoTrue()).thenReturn(List.of(pecaSalva));

            mockMvc.perform(get("/pecas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(PECA_ID.toString())))
                    .andExpect(jsonPath("$[0].codigo", is("FILTRO-01")))
                    .andExpect(jsonPath("$[0].ativo", is(true)));
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há peças ativas")
        void deveRetornarListaVazia() throws Exception {
            when(pecaRepository.findByAtivoTrue()).thenReturn(List.of());

            mockMvc.perform(get("/pecas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /pecas/todas - Listar todas as peças")
    class ListarTodas {

        @Test
        @DisplayName("deve retornar 200 com todas as peças incluindo inativas")
        void deveListarTodas() throws Exception {
            Peca inativa = new Peca();
            inativa.setId(UUID.randomUUID());
            inativa.setCodigo("VELA-01");
            inativa.setNome("Vela de Ignição");
            inativa.setPreco(new BigDecimal("25.00"));
            inativa.setQuantidadeEstoque(0);
            inativa.setEstoqueMinimo(1);
            inativa.setUnidadeMedida("UN");
            inativa.setAtivo(false);
            inativa.setCriadoEm(LocalDateTime.now());

            when(pecaRepository.findAll()).thenReturn(List.of(pecaSalva, inativa));

            mockMvc.perform(get("/pecas/todas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].ativo", is(true)))
                    .andExpect(jsonPath("$[1].ativo", is(false)));
        }
    }

    @Nested
    @DisplayName("GET /pecas/{id} - Buscar peça por ID")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar 200 com peça quando ID existe")
        void deveRetornarPeca() throws Exception {
            when(pecaRepository.findById(PECA_ID)).thenReturn(Optional.of(pecaSalva));

            mockMvc.perform(get("/pecas/{id}", PECA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(PECA_ID.toString())))
                    .andExpect(jsonPath("$.codigo", is("FILTRO-01")))
                    .andExpect(jsonPath("$.nome", is("Filtro de Óleo")));
        }

        @Test
        @DisplayName("deve retornar 404 quando ID não existe")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(pecaRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/pecas/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }
    }

    @Nested
    @DisplayName("POST /pecas - Criar peça")
    class CriarPeca {

        @Test
        @DisplayName("deve criar peça e retornar 201")
        void deveCriarPeca() throws Exception {
            when(pecaRepository.save(any(Peca.class))).thenReturn(pecaSalva);

            mockMvc.perform(post("/pecas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(PECA_ID.toString())))
                    .andExpect(jsonPath("$.codigo", is("FILTRO-01")));
        }

        @Test
        @DisplayName("deve retornar 400 quando nome está em branco")
        void deveRetornar400QuandoNomeEmBranco() throws Exception {
            PecaRequest invalida = new PecaRequest("FILTRO-01", "", null,
                    new BigDecimal("45.90"), 10, 2, "UN");

            mockMvc.perform(post("/pecas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalida)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /pecas/{id} - Atualizar peça")
    class AtualizarPeca {

        @Test
        @DisplayName("deve atualizar peça e retornar 200")
        void deveAtualizarPeca() throws Exception {
            when(pecaRepository.findById(PECA_ID)).thenReturn(Optional.of(pecaSalva));
            when(pecaRepository.save(any(Peca.class))).thenReturn(pecaSalva);

            mockMvc.perform(put("/pecas/{id}", PECA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.codigo", is("FILTRO-01")));
        }

        @Test
        @DisplayName("deve retornar 404 quando peça não existe")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(pecaRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(put("/pecas/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido())))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /pecas/{id}/estoque - Movimentar estoque")
    class MovimentarEstoque {

        @Test
        @DisplayName("deve registrar entrada de estoque e retornar 200")
        void deveRegistrarEntrada() throws Exception {
            when(pecaRepository.findById(PECA_ID)).thenReturn(Optional.of(pecaSalva));
            when(pecaRepository.save(any(Peca.class))).thenReturn(pecaSalva);

            MovimentacaoEstoqueRequest req = new MovimentacaoEstoqueRequest(
                    MovimentacaoEstoqueRequest.Operacao.ENTRADA, 5);

            mockMvc.perform(patch("/pecas/{id}/estoque", PECA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 422 quando estoque insuficiente para saída")
        void deveRetornar422QuandoEstoqueInsuficiente() throws Exception {
            Peca semEstoque = new Peca();
            semEstoque.setId(PECA_ID);
            semEstoque.setCodigo("FILTRO-01");
            semEstoque.setNome("Filtro de Óleo");
            semEstoque.setPreco(new BigDecimal("45.90"));
            semEstoque.setQuantidadeEstoque(1);
            semEstoque.setEstoqueMinimo(2);
            semEstoque.setUnidadeMedida("UN");
            semEstoque.setAtivo(true);
            semEstoque.setCriadoEm(LocalDateTime.now());

            when(pecaRepository.findById(PECA_ID)).thenReturn(Optional.of(semEstoque));

            MovimentacaoEstoqueRequest req = new MovimentacaoEstoqueRequest(
                    MovimentacaoEstoqueRequest.Operacao.SAIDA, 5);

            mockMvc.perform(patch("/pecas/{id}/estoque", PECA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("deve retornar 400 quando quantidade é zero")
        void deveRetornar400QuandoQuantidadeZero() throws Exception {
            MovimentacaoEstoqueRequest invalida = new MovimentacaoEstoqueRequest(
                    MovimentacaoEstoqueRequest.Operacao.ENTRADA, 0);

            mockMvc.perform(patch("/pecas/{id}/estoque", PECA_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalida)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 404 quando peça não encontrada")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(pecaRepository.findById(id)).thenReturn(Optional.empty());

            MovimentacaoEstoqueRequest req = new MovimentacaoEstoqueRequest(
                    MovimentacaoEstoqueRequest.Operacao.ENTRADA, 5);

            mockMvc.perform(patch("/pecas/{id}/estoque", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /pecas/{id} - Desativar peça")
    class DesativarPeca {

        @Test
        @DisplayName("deve desativar peça e retornar 204")
        void deveDesativarPeca() throws Exception {
            when(pecaRepository.findById(PECA_ID)).thenReturn(Optional.of(pecaSalva));
            when(pecaRepository.save(any(Peca.class))).thenReturn(pecaSalva);

            mockMvc.perform(delete("/pecas/{id}", PECA_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 quando peça não encontrada")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(pecaRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/pecas/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }
}
