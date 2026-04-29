package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.ServicoInsumoRequest;
import br.com.fiap.siase.dto.request.ServicoRequest;
import br.com.fiap.siase.dto.response.ServicoResponse;
import br.com.fiap.siase.service.ServicoService;
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
@RequestMapping("/servicos")
@RequiredArgsConstructor
@Tag(name = "Serviços", description = "Catálogo de serviços da oficina com seus insumos necessários")
public class ServicoController {

    private final ServicoService service;

    @GetMapping
    @Operation(summary = "Listar serviços ativos", description = "Retorna apenas os serviços com status ativo.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<ServicoResponse>> listar() {
        return ResponseEntity.ok(service.listarAtivos());
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos os serviços", description = "Retorna todos os serviços, incluindo os desativados.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<ServicoResponse>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar serviço por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serviço encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    public ResponseEntity<ServicoResponse> buscar(@Parameter(description = "ID do serviço") @PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo serviço", description = "Cria um novo serviço no catálogo com preço e tempo estimado de execução.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Serviço criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request) {
        var criado = service.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serviço atualizado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    public ResponseEntity<ServicoResponse> atualizar(
            @Parameter(description = "ID do serviço") @PathVariable UUID id,
            @Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar serviço (soft delete)", description = "Marca o serviço como inativo. Serviços desativados não podem ser adicionados a novas OS.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Serviço desativado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    public ResponseEntity<Void> desativar(@Parameter(description = "ID do serviço") @PathVariable UUID id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/insumos")
    @Operation(
        summary = "Vincular insumo ao serviço",
        description = "Associa uma peça/insumo ao serviço com a quantidade necessária para execução."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Insumo vinculado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Serviço ou peça não encontrados"),
        @ApiResponse(responseCode = "409", description = "Peça já vinculada a este serviço")
    })
    public ResponseEntity<ServicoResponse> adicionarInsumo(
            @Parameter(description = "ID do serviço") @PathVariable UUID id,
            @Valid @RequestBody ServicoInsumoRequest request) {
        return ResponseEntity.ok(service.adicionarInsumo(id, request));
    }

    @PutMapping("/{id}/insumos/{pecaId}")
    @Operation(summary = "Atualizar quantidade de insumo", description = "Altera a quantidade necessária de uma peça já vinculada ao serviço.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quantidade atualizada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Serviço, peça ou vínculo não encontrado")
    })
    public ResponseEntity<ServicoResponse> atualizarInsumo(
            @Parameter(description = "ID do serviço") @PathVariable UUID id,
            @Parameter(description = "ID da peça") @PathVariable UUID pecaId,
            @Valid @RequestBody ServicoInsumoRequest request) {
        return ResponseEntity.ok(service.atualizarInsumo(id, pecaId, request));
    }

    @DeleteMapping("/{id}/insumos/{pecaId}")
    @Operation(summary = "Remover insumo do serviço", description = "Desvincula a peça do serviço.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Insumo removido"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Serviço, peça ou vínculo não encontrado")
    })
    public ResponseEntity<ServicoResponse> removerInsumo(
            @Parameter(description = "ID do serviço") @PathVariable UUID id,
            @Parameter(description = "ID da peça") @PathVariable UUID pecaId) {
        return ResponseEntity.ok(service.removerInsumo(id, pecaId));
    }
}
