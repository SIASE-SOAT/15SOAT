package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.MovimentacaoEstoqueRequest;
import br.com.fiap.siase.dto.request.PecaRequest;
import br.com.fiap.siase.dto.response.PecaResponse;
import br.com.fiap.siase.service.PecaService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pecas")
@RequiredArgsConstructor
@Tag(name = "Peças e Insumos", description = "Catálogo de peças e insumos com controle de estoque")
public class PecaController {

    private final PecaService service;

    @GetMapping
    @Operation(summary = "Listar peças ativas", description = "Retorna apenas as peças com status ativo.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<PecaResponse>> listar() {
        return ResponseEntity.ok(service.listarAtivas());
    }

    @GetMapping("/todas")
    @Operation(summary = "Listar todas as peças", description = "Retorna todas as peças, incluindo as desativadas.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<PecaResponse>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar peça por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Peça encontrada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada")
    })
    public ResponseEntity<PecaResponse> buscar(@Parameter(description = "ID da peça") @PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar nova peça", description = "Cria uma nova peça ou insumo no catálogo com estoque inicial.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Peça criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "409", description = "Código da peça já existe")
    })
    public ResponseEntity<PecaResponse> criar(@Valid @RequestBody PecaRequest request) {
        var criada = service.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar peça")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Peça atualizada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada")
    })
    public ResponseEntity<PecaResponse> atualizar(
            @Parameter(description = "ID da peça") @PathVariable UUID id,
            @Valid @RequestBody PecaRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @PatchMapping("/{id}/estoque")
    @Operation(
        summary = "Movimentar estoque",
        description = "Registra uma movimentação de ENTRADA (recebimento de mercadoria) ou SAIDA (consumo manual) no estoque da peça."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estoque atualizado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada"),
        @ApiResponse(responseCode = "422", description = "Estoque insuficiente para saída")
    })
    public ResponseEntity<PecaResponse> movimentarEstoque(
            @Parameter(description = "ID da peça") @PathVariable UUID id,
            @Valid @RequestBody MovimentacaoEstoqueRequest request) {
        return ResponseEntity.ok(service.movimentarEstoque(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar peça (soft delete)", description = "Marca a peça como inativa. Peças desativadas não podem ser adicionadas a novas OS.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Peça desativada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada")
    })
    public ResponseEntity<Void> desativar(@Parameter(description = "ID da peça") @PathVariable UUID id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
