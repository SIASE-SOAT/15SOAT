package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.DuplicateResourceException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.siase.application.usecase.port.ConsultarStatusOSUCPort;
import br.com.fiap.siase.application.usecase.port.CriarOrdemServicoUCPort;
import br.com.fiap.siase.application.usecase.port.ListarOrdensServicoUCPort;
import br.com.fiap.siase.application.usecase.port.AprovarOrcamentoUCPort;
import br.com.fiap.siase.application.usecase.port.AvancarStatusUCPort;
import br.com.fiap.siase.application.usecase.port.CancelarOrdemUCPort;
import br.com.fiap.siase.application.usecase.port.AdicionarPecaUCPort;
import br.com.fiap.siase.application.usecase.port.AdicionarServicoUCPort;
import br.com.fiap.siase.application.usecase.port.ConsultarTempoMedioUCPort;
import br.com.fiap.siase.application.usecase.port.PrepararAberturaOSUCPort;
import br.com.fiap.siase.application.usecase.port.IniciarExecucaoItemUCPort;
import br.com.fiap.siase.application.usecase.port.FinalizarExecucaoItemUCPort;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("GlobalExceptionHandler - Mapeamento de excecoes HTTP")
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;

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
    @MockBean ClienteRepositoryPort clienteRepository;

    @Test
    @DisplayName("Deve retornar 404 quando ResourceNotFoundException lancada")
    void deveRetornar404QuandoResourceNotFoundException() throws Exception {
        UUID id = UUID.randomUUID();
        when(consultarStatusOSUC.executar(any()))
                .thenThrow(new ResourceNotFoundException("OS não encontrada: " + id));

        mockMvc.perform(get("/ordens/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("OS não encontrada: " + id))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Deve retornar 422 quando BusinessException lancada")
    void deveRetornar422QuandoBusinessException() throws Exception {
        UUID id = UUID.randomUUID();
        when(consultarStatusOSUC.executar(any()))
                .thenThrow(new BusinessException("Regra de negocio violada"));

        mockMvc.perform(get("/ordens/{id}", id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Regra de negocio violada"));
    }

    @Test
    @DisplayName("Deve retornar 409 quando DuplicateResourceException lancada")
    void deveRetornar409QuandoDuplicateResourceException() throws Exception {
        when(clienteRepository.findByDocumento(any())).thenReturn(java.util.Optional.empty());
        when(clienteRepository.save(any())).thenThrow(new DuplicateResourceException("Documento já cadastrado"));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Teste\",\"tipoPessoa\":\"PF\",\"documento\":\"529.982.247-25\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Deve retornar 400 quando corpo da requisicao e invalido")
    void deveRetornar400QuandoCorpoInvalido() throws Exception {
        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("json-malformado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Deve retornar 400 quando campos obrigatorios estao ausentes")
    void deveRetornar400QuandoCamposObrigatoriosAusentes() throws Exception {
        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
