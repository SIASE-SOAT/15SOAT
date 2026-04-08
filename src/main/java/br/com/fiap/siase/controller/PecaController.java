package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.MovimentacaoEstoqueRequest;
import br.com.fiap.siase.dto.request.PecaRequest;
import br.com.fiap.siase.dto.response.PecaResponse;
import br.com.fiap.siase.service.PecaService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Lista todas as peças ativas")
    public ResponseEntity<List<PecaResponse>> listar() {
        return ResponseEntity.ok(service.listarAtivas());
    }

    @GetMapping("/todas")
    @Operation(summary = "Lista todas as peças, incluindo inativas")
    public ResponseEntity<List<PecaResponse>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca peça por ID")
    public ResponseEntity<PecaResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra uma nova peça ou insumo")
    public ResponseEntity<PecaResponse> criar(@Valid @RequestBody PecaRequest request) {
        var criada = service.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de uma peça")
    public ResponseEntity<PecaResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PecaRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @PatchMapping("/{id}/estoque")
    @Operation(summary = "Movimenta estoque: ENTRADA (recebimento) ou SAIDA (consumo)")
    public ResponseEntity<PecaResponse> movimentarEstoque(
            @PathVariable UUID id,
            @Valid @RequestBody MovimentacaoEstoqueRequest request) {
        return ResponseEntity.ok(service.movimentarEstoque(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativa uma peça (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
