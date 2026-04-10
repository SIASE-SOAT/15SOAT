package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.ServicoInsumoRequest;
import br.com.fiap.siase.dto.request.ServicoRequest;
import br.com.fiap.siase.dto.response.ServicoResponse;
import br.com.fiap.siase.service.ServicoService;
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
@RequestMapping("/servicos")
@RequiredArgsConstructor
@Tag(name = "Serviços", description = "Catálogo de serviços da oficina com seus insumos necessários")
public class ServicoController {

    private final ServicoService service;

    @GetMapping
    @Operation(summary = "Lista todos os serviços ativos")
    public ResponseEntity<List<ServicoResponse>> listar() {
        return ResponseEntity.ok(service.listarAtivos());
    }

    @GetMapping("/todos")
    @Operation(summary = "Lista todos os serviços, incluindo inativos")
    public ResponseEntity<List<ServicoResponse>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca serviço por ID")
    public ResponseEntity<ServicoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo serviço")
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request) {
        var criado = service.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um serviço")
    public ResponseEntity<ServicoResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativa um serviço (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/insumos")
    @Operation(summary = "Vincula uma peça/insumo ao serviço com a quantidade necessária")
    public ResponseEntity<ServicoResponse> adicionarInsumo(
            @PathVariable UUID id,
            @Valid @RequestBody ServicoInsumoRequest request) {
        return ResponseEntity.ok(service.adicionarInsumo(id, request));
    }

    @PutMapping("/{id}/insumos/{pecaId}")
    @Operation(summary = "Atualiza a quantidade de uma peça vinculada ao serviço")
    public ResponseEntity<ServicoResponse> atualizarInsumo(
            @PathVariable UUID id,
            @PathVariable UUID pecaId,
            @Valid @RequestBody ServicoInsumoRequest request) {
        return ResponseEntity.ok(service.atualizarInsumo(id, pecaId, request));
    }

    @DeleteMapping("/{id}/insumos/{pecaId}")
    @Operation(summary = "Remove a vinculação de uma peça do serviço")
    public ResponseEntity<ServicoResponse> removerInsumo(
            @PathVariable UUID id,
            @PathVariable UUID pecaId) {
        return ResponseEntity.ok(service.removerInsumo(id, pecaId));
    }
}
