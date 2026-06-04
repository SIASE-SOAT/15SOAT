package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.domain.model.Peca;
import br.com.fiap.siase.domain.model.Servico;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ServicoController - Endpoints REST")
class ServicoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ServicoRepositoryPort servicoRepository;
    @MockBean PecaRepositoryPort pecaRepository;

    private static final UUID SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private Servico servicoAtivo;

    @BeforeEach
    void setUp() {
        servicoAtivo = new Servico();
        servicoAtivo.setId(SERVICO_ID);
        servicoAtivo.setNome("Troca de Óleo");
        servicoAtivo.setDescricao("Troca completa de óleo e filtro");
        servicoAtivo.setPreco(new BigDecimal("150.00"));
        servicoAtivo.setTempoEstimadoMinutos(30);
        servicoAtivo.setAtivo(true);
    }

    @Nested
    @DisplayName("GET /servicos/todos - listar todos")
    class ListarTodosServicos {

        @Test
        @DisplayName("deve retornar 200 com todos servicos")
        void deveListarTodos() throws Exception {
            when(servicoRepository.findAll()).thenReturn(List.of(servicoAtivo));

            mockMvc.perform(get("/servicos/todos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("POST /servicos - criar servico")
    class CriarServico {

        @Test
        @DisplayName("deve criar servico com sucesso")
        void deveCriarServico() throws Exception {
            when(servicoRepository.save(any())).thenReturn(servicoAtivo);

            String body = """
                {"nome":"Troca de Oleo","descricao":"Servico completo","preco":150.00}
                """;

            mockMvc.perform(post("/servicos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome", is("Troca de Óleo")));
        }

        @Test
        @DisplayName("deve retornar 400 com dados invalidos")
        void deveRetornar400DadosInvalidos() throws Exception {
            String body = "{\"nome\":\"\",\"preco\":-1}";

            mockMvc.perform(post("/servicos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /servicos/{id} - atualizar servico")
    class AtualizarServico {

        @Test
        @DisplayName("deve atualizar servico com sucesso")
        void deveAtualizarServico() throws Exception {
            when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servicoAtivo));
            when(servicoRepository.save(any())).thenReturn(servicoAtivo);

            String body = """
                {"nome":"Troca de Oleo Atualizada","descricao":"Nova","preco":200.00}
                """;

            mockMvc.perform(put("/servicos/{id}", SERVICO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 404 quando servico nao encontrado")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(servicoRepository.findById(id)).thenReturn(Optional.empty());

            String body = """
                {"nome":"X","descricao":"Y","preco":100.00}
                """;

            mockMvc.perform(put("/servicos/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /servicos/{id} - desativar servico")
    class DesativarServico {

        @Test
        @DisplayName("deve desativar servico com sucesso")
        void deveDesativarServico() throws Exception {
            when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servicoAtivo));
            when(servicoRepository.save(any())).thenReturn(servicoAtivo);

            mockMvc.perform(delete("/servicos/{id}", SERVICO_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 quando servico nao encontrado")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(servicoRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/servicos/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /servicos/{id}/insumos - adicionar insumo")
    class AdicionarInsumo {

        private static final UUID PECA_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
        private Peca peca;

        @BeforeEach
        void setUp() {
            peca = new Peca();
            peca.setId(PECA_ID);
            peca.setCodigo("PEC-001");
            peca.setNome("Filtro de Oleo");
            peca.setPreco(new BigDecimal("45.00"));
            peca.setQuantidadeEstoque(10);
            peca.setUnidadeMedida("UN");
            peca.setAtivo(true);
        }

        @Test
        @DisplayName("deve adicionar insumo com sucesso")
        void deveAdicionarInsumo() throws Exception {
            when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servicoAtivo));
            when(pecaRepository.findById(PECA_ID)).thenReturn(Optional.of(peca));
            when(servicoRepository.save(any())).thenReturn(servicoAtivo);

            String body = "{\"pecaId\":\"" + PECA_ID + "\",\"quantidade\":2}";

            mockMvc.perform(post("/servicos/{id}/insumos", SERVICO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 404 quando peca nao encontrada")
        void deveRetornar404PecaNaoEncontrada() throws Exception {
            when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servicoAtivo));
            when(pecaRepository.findById(PECA_ID)).thenReturn(Optional.empty());

            String body = "{\"pecaId\":\"" + PECA_ID + "\",\"quantidade\":2}";

            mockMvc.perform(post("/servicos/{id}/insumos", SERVICO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /servicos/{id}/insumos/{pecaId} - atualizar insumo")
    class AtualizarInsumo {

        @Test
        @DisplayName("deve retornar 404 quando insumo nao vinculado")
        void deveRetornar404InsumoNaoVinculado() throws Exception {
            UUID pecaId = UUID.randomUUID();
            when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servicoAtivo));

            String body = "{\"pecaId\":\"" + pecaId + "\",\"quantidade\":5}";

            mockMvc.perform(put("/servicos/{id}/insumos/{pecaId}", SERVICO_ID, pecaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /servicos/{id}/insumos/{pecaId} - remover insumo")
    class RemoverInsumo {

        @Test
        @DisplayName("deve retornar 404 quando insumo nao vinculado")
        void deveRetornar404InsumoNaoVinculado() throws Exception {
            UUID pecaId = UUID.randomUUID();
            when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servicoAtivo));

            mockMvc.perform(delete("/servicos/{id}/insumos/{pecaId}", SERVICO_ID, pecaId))
                    .andExpect(status().isNotFound());
        }
    }
}
