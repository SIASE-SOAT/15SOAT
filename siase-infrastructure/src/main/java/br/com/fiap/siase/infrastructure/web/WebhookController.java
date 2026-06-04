package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.AtualizarStatusWebhookRequest;
import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.AtualizarStatusViaWebhookUC;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ordens/webhook")
@Tag(name = "Ordens de Serviço")
public class WebhookController {

    private final AtualizarStatusViaWebhookUC atualizarStatusViaWebhookUC;

    public WebhookController(AtualizarStatusViaWebhookUC atualizarStatusViaWebhookUC) {
        this.atualizarStatusViaWebhookUC = atualizarStatusViaWebhookUC;
    }

    @PostMapping("/status")
    @Operation(summary = "Atualizar status via webhook (integração externa)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "422", description = "Token inválido ou transição de status inválida")
    })
    public ResponseEntity<OrdemDeServicoResponse> atualizarStatus(
            @Valid @RequestBody AtualizarStatusWebhookRequest request) {
        OrdemDeServicoResponse response = atualizarStatusViaWebhookUC.executar(
                request.numero(),
                request.novoStatus(),
                request.tokenExterno()
        );
        return ResponseEntity.ok(response);
    }
}
