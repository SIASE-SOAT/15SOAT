package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.AtualizarStatusViaWebhookUC;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("WebhookController - Atualizacao de status via webhook")
class WebhookControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AtualizarStatusViaWebhookUC atualizarStatusViaWebhookUC;

    private OrdemDeServicoResponse buildResponse(StatusOS status) {
        return new OrdemDeServicoResponse(
                UUID.randomUUID(), "OS-20240101-ABC123", UUID.randomUUID(), "João Silva",
                UUID.randomUUID(), "ABC1234", "Honda Civic",
                status.name(), status.getDescricao(), null,
                List.of(), List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Deve atualizar status via webhook e retornar 200")
    void deveAtualizarStatusComSucesso() throws Exception {
        when(atualizarStatusViaWebhookUC.executar(anyString(), anyString(), anyString()))
                .thenReturn(buildResponse(StatusOS.EM_DIAGNOSTICO));

        String body = """
                {"numero":"OS-20240101-ABC123","novoStatus":"EM_DIAGNOSTICO","tokenExterno":"token-valido"}
                """;

        mockMvc.perform(post("/ordens/webhook/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
    }

    @Test
    @DisplayName("Deve retornar 422 quando token invalido")
    void deveRetornar422QuandoTokenInvalido() throws Exception {
        when(atualizarStatusViaWebhookUC.executar(anyString(), anyString(), anyString()))
                .thenThrow(new BusinessException("Token de serviço externo inválido."));

        String body = """
                {"numero":"OS-20240101-ABC123","novoStatus":"EM_DIAGNOSTICO","tokenExterno":"token-errado"}
                """;

        mockMvc.perform(post("/ordens/webhook/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("Deve retornar 400 quando campos obrigatorios ausentes")
    void deveRetornar400QuandoCamposAusentes() throws Exception {
        mockMvc.perform(post("/ordens/webhook/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
