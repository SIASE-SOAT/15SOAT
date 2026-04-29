package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.PagamentoRequest;
import br.com.fiap.siase.dto.response.PagamentoResponse;
import br.com.fiap.siase.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Registro e ciclo de vida de pagamentos vinculados a ordens de serviço")
public class PagamentoController {

    private final PagamentoService service;

    @PostMapping("/ordens/{osId}/pagamento")
    @Operation(
        summary = "Registrar pagamento",
        description = "Cria um pagamento PENDENTE para uma OS no status FINALIZADA. Cada OS admite apenas um pagamento ativo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pagamento registrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "422", description = "OS não está FINALIZADA ou já possui pagamento ativo")
    })
    public ResponseEntity<PagamentoResponse> registrar(
            @Parameter(description = "ID da ordem de serviço") @PathVariable UUID osId,
            @Valid @RequestBody PagamentoRequest request) {
        PagamentoResponse response = service.registrar(osId, request);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/pagamentos/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/ordens/{osId}/pagamento")
    @Operation(
        summary = "Buscar pagamento da OS",
        description = "Retorna o pagamento associado à ordem de serviço informada."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Nenhum pagamento encontrado para a OS")
    })
    public ResponseEntity<PagamentoResponse> buscarPorOS(
            @Parameter(description = "ID da ordem de serviço") @PathVariable UUID osId) {
        return ResponseEntity.ok(service.buscarPorOS(osId));
    }

    @PatchMapping("/pagamentos/{id}/confirmar")
    @Operation(
        summary = "Confirmar pagamento",
        description = "Muda o status do pagamento de PENDENTE para PAGO e avança a OS para ENTREGUE."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagamento confirmado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado"),
        @ApiResponse(responseCode = "422", description = "Apenas pagamentos PENDENTES podem ser confirmados")
    })
    public ResponseEntity<PagamentoResponse> confirmar(
            @Parameter(description = "ID do pagamento") @PathVariable UUID id) {
        return ResponseEntity.ok(service.confirmar(id));
    }

    @PatchMapping("/pagamentos/{id}/cancelar")
    @Operation(
        summary = "Cancelar pagamento",
        description = "Cancela um pagamento PENDENTE, liberando a OS para receber um novo pagamento."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagamento cancelado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado"),
        @ApiResponse(responseCode = "422", description = "Pagamentos já confirmados não podem ser cancelados")
    })
    public ResponseEntity<PagamentoResponse> cancelar(
            @Parameter(description = "ID do pagamento") @PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelar(id));
    }
}
