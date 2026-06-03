package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.ItemPecaRequest;
import br.com.fiap.siase.application.dto.input.ItemServicoRequest;
import br.com.fiap.siase.application.dto.input.OrdemDeServicoRequest;
import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.dto.output.PreparacaoAberturaOrdemResponse;
import br.com.fiap.siase.application.usecase.port.AdicionarPecaUCPort;
import br.com.fiap.siase.application.usecase.port.AdicionarServicoUCPort;
import br.com.fiap.siase.application.usecase.port.AprovarOrcamentoUCPort;
import br.com.fiap.siase.application.usecase.port.AvancarStatusUCPort;
import br.com.fiap.siase.application.usecase.port.CancelarOrdemUCPort;
import br.com.fiap.siase.application.usecase.port.ConsultarStatusOSUCPort;
import br.com.fiap.siase.application.usecase.port.ConsultarTempoMedioUCPort;
import br.com.fiap.siase.application.usecase.port.CriarOrdemServicoUCPort;
import br.com.fiap.siase.application.usecase.port.FinalizarExecucaoItemUCPort;
import br.com.fiap.siase.application.usecase.port.IniciarExecucaoItemUCPort;
import br.com.fiap.siase.application.usecase.port.ListarOrdensServicoUCPort;
import br.com.fiap.siase.application.usecase.port.PrepararAberturaOSUCPort;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("OrdemServicoController - Endpoints REST")
class OrdemServicoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CriarOrdemServicoUCPort criarOrdemServicoUC;
    @MockBean ListarOrdensServicoUCPort listarOrdensServicoUC;
    @MockBean ConsultarStatusOSUCPort consultarStatusOSUC;
    @MockBean AprovarOrcamentoUCPort aprovarOrcamentoUC;
    @MockBean AvancarStatusUCPort avancarStatusUC;
    @MockBean CancelarOrdemUCPort cancelarOrdemUC;
    @MockBean AdicionarPecaUCPort adicionarPecaUC;
    @MockBean AdicionarServicoUCPort adicionarServicoUC;
    @MockBean ConsultarTempoMedioUCPort consultarTempoMedioUC;
    @MockBean PrepararAberturaOSUCPort prepararAberturaOSUC;
    @MockBean IniciarExecucaoItemUCPort iniciarExecucaoItemUC;
    @MockBean FinalizarExecucaoItemUCPort finalizarExecucaoItemUC;

    private OrdemDeServicoResponse buildResponse(UUID id, StatusOS status) {
        return new OrdemDeServicoResponse(
                id, "OS-20240101-ABC123", UUID.randomUUID(), "João Silva",
                UUID.randomUUID(), "ABC1234", "Honda Civic",
                status.name(), status.getDescricao(), null,
                List.of(), List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private OrdemDeServicoRequest buildRequest() {
        return new OrdemDeServicoRequest(
                UUID.randomUUID(), UUID.randomUUID(), "Revisão geral",
                List.of(new ItemServicoRequest(UUID.randomUUID(), "obs")),
                List.of()
        );
    }

    @Nested
    @DisplayName("POST /ordens - Criar OS")
    class CriarOS {

        @Test
        @DisplayName("deve criar OS e retornar 201")
        void deveCriarOS() throws Exception {
            UUID id = UUID.randomUUID();
            when(criarOrdemServicoUC.executar(any())).thenReturn(buildResponse(id, StatusOS.RECEBIDA));

            mockMvc.perform(post("/ordens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.status").value("RECEBIDA"));
        }

        @Test
        @DisplayName("deve retornar 422 quando veículo já possui OS")
        void deveRetornar422() throws Exception {
            when(criarOrdemServicoUC.executar(any()))
                    .thenThrow(new BusinessException("Veículo já possui OS em andamento"));

            mockMvc.perform(post("/ordens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Veículo já possui OS em andamento"));
        }

        @Test
        @DisplayName("deve retornar 404 quando cliente não encontrado")
        void deveRetornar404() throws Exception {
            when(criarOrdemServicoUC.executar(any()))
                    .thenThrow(new ResourceNotFoundException("Cliente não encontrado"));

            mockMvc.perform(post("/ordens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /ordens - Listar OS")
    class ListarOS {

        @Test
        @DisplayName("deve listar todas as OS sem filtro")
        void deveListarTodas() throws Exception {
            when(listarOrdensServicoUC.executar(isNull())).thenReturn(List.of(
                    buildResponse(UUID.randomUUID(), StatusOS.RECEBIDA),
                    buildResponse(UUID.randomUUID(), StatusOS.EM_EXECUCAO)
            ));

            mockMvc.perform(get("/ordens"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("deve filtrar OS por status")
        void deveListarPorStatus() throws Exception {
            when(listarOrdensServicoUC.executar(eq(StatusOS.RECEBIDA)))
                    .thenReturn(List.of(buildResponse(UUID.randomUUID(), StatusOS.RECEBIDA)));

            mockMvc.perform(get("/ordens").param("status", "RECEBIDA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("RECEBIDA"));
        }

        @Test
        @DisplayName("deve retornar lista vazia")
        void deveRetornarListaVazia() throws Exception {
            when(listarOrdensServicoUC.executar(isNull())).thenReturn(List.of());

            mockMvc.perform(get("/ordens"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /ordens/{id} - Buscar OS por ID")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar 200 com OS quando encontrada")
        void deveRetornarOS() throws Exception {
            UUID id = UUID.randomUUID();
            when(consultarStatusOSUC.executar(eq(id))).thenReturn(buildResponse(id, StatusOS.EM_DIAGNOSTICO));

            mockMvc.perform(get("/ordens/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
        }

        @Test
        @DisplayName("deve retornar 404 quando OS não encontrada")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(consultarStatusOSUC.executar(eq(id)))
                    .thenThrow(new ResourceNotFoundException("OS não encontrada"));

            mockMvc.perform(get("/ordens/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /ordens/acompanhar/{numero} - Acompanhar (público)")
    class AcompanharOS {

        @Test
        @DisplayName("deve retornar 200 com OS")
        void deveRetornarOS() throws Exception {
            UUID id = UUID.randomUUID();
            when(consultarStatusOSUC.executarPorNumero(anyString()))
                    .thenReturn(buildResponse(id, StatusOS.AGUARDANDO_APROVACAO));

            mockMvc.perform(get("/ordens/acompanhar/OS-20240101-ABC123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"));
        }

        @Test
        @DisplayName("deve retornar 404 quando número não existe")
        void deveRetornar404() throws Exception {
            when(consultarStatusOSUC.executarPorNumero(anyString()))
                    .thenThrow(new ResourceNotFoundException("OS não encontrada"));

            mockMvc.perform(get("/ordens/acompanhar/OS-INVALIDO"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /ordens/acompanhar/{numero}/aprovar-orcamento - Aprovar (público)")
    class AprovarOrcamento {

        @Test
        @DisplayName("deve aprovar orçamento e retornar 200")
        void deveAprovar() throws Exception {
            when(aprovarOrcamentoUC.aprovar(anyString()))
                    .thenReturn(buildResponse(UUID.randomUUID(), StatusOS.APROVADO));

            mockMvc.perform(patch("/ordens/acompanhar/OS-20240101-ABC123/aprovar-orcamento"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APROVADO"));
        }

        @Test
        @DisplayName("deve retornar 422 quando OS não aguarda aprovação")
        void deveRetornar422() throws Exception {
            when(aprovarOrcamentoUC.aprovar(anyString()))
                    .thenThrow(new BusinessException("OS não está aguardando aprovação"));

            mockMvc.perform(patch("/ordens/acompanhar/OS-20240101-ABC123/aprovar-orcamento"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("OS não está aguardando aprovação"));
        }
    }

    @Nested
    @DisplayName("PATCH /ordens/acompanhar/{numero}/recusar-orcamento - Recusar (público)")
    class RecusarOrcamento {

        @Test
        @DisplayName("deve recusar orçamento e retornar 200")
        void deveRecusar() throws Exception {
            when(aprovarOrcamentoUC.recusar(anyString()))
                    .thenReturn(buildResponse(UUID.randomUUID(), StatusOS.CANCELADA));

            mockMvc.perform(patch("/ordens/acompanhar/OS-20240101-ABC123/recusar-orcamento"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADA"));
        }

        @Test
        @DisplayName("deve retornar 422 quando recusa não é permitida")
        void deveRetornar422() throws Exception {
            when(aprovarOrcamentoUC.recusar(anyString()))
                    .thenThrow(new BusinessException("Operação não permitida"));

            mockMvc.perform(patch("/ordens/acompanhar/OS-20240101-ABC123/recusar-orcamento"))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("PATCH /ordens/{id}/avancar - Avançar Status")
    class AvancarStatus {

        @Test
        @DisplayName("deve avançar status e retornar 200")
        void deveAvancar() throws Exception {
            UUID id = UUID.randomUUID();
            when(avancarStatusUC.executar(eq(id))).thenReturn(buildResponse(id, StatusOS.EM_DIAGNOSTICO));

            mockMvc.perform(patch("/ordens/{id}/avancar", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
        }

        @Test
        @DisplayName("deve retornar 422 quando OS já entregue")
        void deveRetornar422() throws Exception {
            UUID id = UUID.randomUUID();
            when(avancarStatusUC.executar(eq(id)))
                    .thenThrow(new BusinessException("OS já foi entregue"));

            mockMvc.perform(patch("/ordens/{id}/avancar", id))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("OS já foi entregue"));
        }

        @Test
        @DisplayName("deve retornar 404 quando OS não encontrada")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(avancarStatusUC.executar(eq(id)))
                    .thenThrow(new ResourceNotFoundException("OS não encontrada"));

            mockMvc.perform(patch("/ordens/{id}/avancar", id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /ordens/{id}/cancelar - Cancelar OS")
    class CancelarOS {

        @Test
        @DisplayName("deve cancelar OS e retornar 200")
        void deveCancelar() throws Exception {
            UUID id = UUID.randomUUID();
            when(cancelarOrdemUC.executar(eq(id))).thenReturn(buildResponse(id, StatusOS.CANCELADA));

            mockMvc.perform(patch("/ordens/{id}/cancelar", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADA"));
        }

        @Test
        @DisplayName("deve retornar 422 quando cancelamento não permitido")
        void deveRetornar422() throws Exception {
            UUID id = UUID.randomUUID();
            when(cancelarOrdemUC.executar(eq(id)))
                    .thenThrow(new BusinessException("OS não pode ser cancelada"));

            mockMvc.perform(patch("/ordens/{id}/cancelar", id))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("deve retornar 404 quando OS não encontrada")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(cancelarOrdemUC.executar(eq(id)))
                    .thenThrow(new ResourceNotFoundException("OS não encontrada"));

            mockMvc.perform(patch("/ordens/{id}/cancelar", id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /ordens/monitoramento/tempo-medio - Tempo Médio")
    class TempoMedio {

        @Test
        @DisplayName("deve retornar 200 com tempo médio")
        void deveRetornarTempoMedio() throws Exception {
            when(consultarTempoMedioUC.executar()).thenReturn(120.0);

            mockMvc.perform(get("/ordens/monitoramento/tempo-medio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tempoMedioMinutos").value(120.0))
                    .andExpect(jsonPath("$.tempoMedioHoras").value(2.0))
                    .andExpect(jsonPath("$.descricao").value("Tempo médio de execução dos serviços finalizados"));
        }

        @Test
        @DisplayName("deve retornar zero quando não há OS finalizadas")
        void deveRetornarZero() throws Exception {
            when(consultarTempoMedioUC.executar()).thenReturn(null);

            mockMvc.perform(get("/ordens/monitoramento/tempo-medio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tempoMedioMinutos").value(0.0))
                    .andExpect(jsonPath("$.tempoMedioHoras").value(0.0));
        }
    }

    @Nested
    @DisplayName("POST /ordens/{id}/items-peca - Adicionar Peça")
    class AdicionarPeca {

        @Test
        @DisplayName("deve adicionar peça e retornar 200")
        void deveAdicionarPeca() throws Exception {
            UUID osId = UUID.randomUUID();
            ItemPecaRequest req = new ItemPecaRequest(UUID.randomUUID(), 2);
            when(adicionarPecaUC.executar(eq(osId), any())).thenReturn(buildResponse(osId, StatusOS.RECEBIDA));

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 422 quando estoque insuficiente")
        void deveRetornar422() throws Exception {
            UUID osId = UUID.randomUUID();
            ItemPecaRequest req = new ItemPecaRequest(UUID.randomUUID(), 100);
            when(adicionarPecaUC.executar(eq(osId), any()))
                    .thenThrow(new BusinessException("Estoque insuficiente"));

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("deve retornar 404 quando peça não encontrada")
        void deveRetornar404() throws Exception {
            UUID osId = UUID.randomUUID();
            ItemPecaRequest req = new ItemPecaRequest(UUID.randomUUID(), 1);
            when(adicionarPecaUC.executar(eq(osId), any()))
                    .thenThrow(new ResourceNotFoundException("Peça não encontrada"));

            mockMvc.perform(post("/ordens/{id}/items-peca", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /ordens/{id}/items-servico - Adicionar Serviço")
    class AdicionarServico {

        @Test
        @DisplayName("deve adicionar serviço e retornar 200")
        void deveAdicionarServico() throws Exception {
            UUID osId = UUID.randomUUID();
            ItemServicoRequest req = new ItemServicoRequest(UUID.randomUUID(), "obs");
            when(adicionarServicoUC.executar(eq(osId), any())).thenReturn(buildResponse(osId, StatusOS.RECEBIDA));

            mockMvc.perform(post("/ordens/{id}/items-servico", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 422 quando serviço já adicionado")
        void deveRetornar422() throws Exception {
            UUID osId = UUID.randomUUID();
            ItemServicoRequest req = new ItemServicoRequest(UUID.randomUUID(), null);
            when(adicionarServicoUC.executar(eq(osId), any()))
                    .thenThrow(new BusinessException("Serviço já adicionado"));

            mockMvc.perform(post("/ordens/{id}/items-servico", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("GET /ordens/preparar-abertura - Preparar Abertura")
    class PrepararAbertura {

        @Test
        @DisplayName("deve retornar 200 com dados de preparação")
        void deveRetornarPreparacao() throws Exception {
            var cliente = new PreparacaoAberturaOrdemResponse.ClienteIdentificadoResponse(
                    UUID.randomUUID(), "João Silva", "12345678901", "joao@email.com", "11999999999");
            var veiculo = new PreparacaoAberturaOrdemResponse.VeiculoIdentificadoResponse(
                    UUID.randomUUID(), "ABC1234", "Honda", "Civic", 2020, true);
            var response = new PreparacaoAberturaOrdemResponse(cliente, List.of(veiculo), null, false);
            when(prepararAberturaOSUC.executar(eq("12345678901"), isNull())).thenReturn(response);

            mockMvc.perform(get("/ordens/preparar-abertura").param("documento", "12345678901"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cliente.nome").value("João Silva"));
        }

        @Test
        @DisplayName("deve retornar 404 quando cliente não encontrado")
        void deveRetornar404() throws Exception {
            when(prepararAberturaOSUC.executar(anyString(), isNull()))
                    .thenThrow(new ResourceNotFoundException("Cliente não encontrado"));

            mockMvc.perform(get("/ordens/preparar-abertura").param("documento", "00000000000"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /ordens/{id}/itens-servico/{itemId}/iniciar - Iniciar Execução")
    class IniciarExecucao {

        @Test
        @DisplayName("deve iniciar execução e retornar 200")
        void deveIniciar() throws Exception {
            UUID osId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            when(iniciarExecucaoItemUC.executar(eq(osId), eq(itemId)))
                    .thenReturn(buildResponse(osId, StatusOS.EM_EXECUCAO));

            mockMvc.perform(patch("/ordens/{id}/itens-servico/{itemId}/iniciar", osId, itemId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
        }

        @Test
        @DisplayName("deve retornar 422 quando OS não está em execução")
        void deveRetornar422() throws Exception {
            UUID osId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            when(iniciarExecucaoItemUC.executar(eq(osId), eq(itemId)))
                    .thenThrow(new BusinessException("OS não está em execução"));

            mockMvc.perform(patch("/ordens/{id}/itens-servico/{itemId}/iniciar", osId, itemId))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("OS não está em execução"));
        }

        @Test
        @DisplayName("deve retornar 404 quando item não encontrado")
        void deveRetornar404() throws Exception {
            UUID osId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            when(iniciarExecucaoItemUC.executar(eq(osId), eq(itemId)))
                    .thenThrow(new ResourceNotFoundException("Item não encontrado"));

            mockMvc.perform(patch("/ordens/{id}/itens-servico/{itemId}/iniciar", osId, itemId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /ordens/{id}/itens-servico/{itemId}/finalizar - Finalizar Execução")
    class FinalizarExecucao {

        @Test
        @DisplayName("deve finalizar execução e retornar 200")
        void deveFinalizar() throws Exception {
            UUID osId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            when(finalizarExecucaoItemUC.executar(eq(osId), eq(itemId)))
                    .thenReturn(buildResponse(osId, StatusOS.EM_EXECUCAO));

            mockMvc.perform(patch("/ordens/{id}/itens-servico/{itemId}/finalizar", osId, itemId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
        }

        @Test
        @DisplayName("deve retornar 422 quando OS não está em execução")
        void deveRetornar422() throws Exception {
            UUID osId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            when(finalizarExecucaoItemUC.executar(eq(osId), eq(itemId)))
                    .thenThrow(new BusinessException("OS não está em execução"));

            mockMvc.perform(patch("/ordens/{id}/itens-servico/{itemId}/finalizar", osId, itemId))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("deve retornar 404 quando OS não encontrada")
        void deveRetornar404() throws Exception {
            UUID osId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            when(finalizarExecucaoItemUC.executar(eq(osId), eq(itemId)))
                    .thenThrow(new ResourceNotFoundException("OS não encontrada"));

            mockMvc.perform(patch("/ordens/{id}/itens-servico/{itemId}/finalizar", osId, itemId))
                    .andExpect(status().isNotFound());
        }
    }
}
