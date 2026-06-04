package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.MovimentacaoEstoqueRequest;
import br.com.fiap.siase.application.dto.input.PecaRequest;
import br.com.fiap.siase.application.dto.output.PecaResponse;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Peca;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pecas")
@Tag(name = "Peças e Insumos", description = "Catálogo de peças e insumos com controle de estoque")
public class PecaController {

    private final PecaRepositoryPort pecaRepository;

    public PecaController(PecaRepositoryPort pecaRepository) {
        this.pecaRepository = pecaRepository;
    }

    @GetMapping
    @Operation(summary = "Listar peças ativas", description = "Retorna apenas as peças com status ativo.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PecaResponse>> listar() {
        return ResponseEntity.ok(
                pecaRepository.findByAtivoTrue().stream()
                        .map(PecaResponse::from)
                        .toList()
        );
    }

    @GetMapping("/todas")
    @Operation(summary = "Listar todas as peças", description = "Retorna todas as peças, incluindo as desativadas.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PecaResponse>> listarTodas() {
        return ResponseEntity.ok(
                pecaRepository.findAll().stream()
                        .map(PecaResponse::from)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar peça por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Peça encontrada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada")
    })
    @Transactional(readOnly = true)
    public ResponseEntity<PecaResponse> buscar(@Parameter(description = "ID da peça") @PathVariable UUID id) {
        return ResponseEntity.ok(PecaResponse.from(findById(id)));
    }

    @PostMapping
    @Operation(summary = "Cadastrar nova peça", description = "Cria uma nova peça ou insumo no catálogo com estoque inicial.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Peça criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "409", description = "Código da peça já existe")
    })
    @Transactional
    public ResponseEntity<PecaResponse> criar(@Valid @RequestBody PecaRequest request) {
        var peca = new Peca();
        peca.setCodigo(request.codigo().toUpperCase().trim());
        peca.setNome(request.nome());
        peca.setDescricao(request.descricao());
        peca.setPreco(request.preco());
        peca.setQuantidadeEstoque(request.quantidadeEstoque() != null ? request.quantidadeEstoque() : 0);
        peca.setEstoqueMinimo(request.estoqueMinimo() != null ? request.estoqueMinimo() : 0);
        peca.setUnidadeMedida(request.unidadeMedida() != null ? request.unidadeMedida().toUpperCase().trim() : "UN");
        var criada = PecaResponse.from(pecaRepository.save(peca));
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
    @Transactional
    public ResponseEntity<PecaResponse> atualizar(
            @Parameter(description = "ID da peça") @PathVariable UUID id,
            @Valid @RequestBody PecaRequest request) {
        var peca = findById(id);
        peca.setCodigo(request.codigo().toUpperCase().trim());
        peca.setNome(request.nome());
        peca.setDescricao(request.descricao());
        peca.setPreco(request.preco());
        peca.setEstoqueMinimo(request.estoqueMinimo() != null ? request.estoqueMinimo() : peca.getEstoqueMinimo());
        peca.setUnidadeMedida(request.unidadeMedida() != null ? request.unidadeMedida().toUpperCase().trim() : peca.getUnidadeMedida());
        return ResponseEntity.ok(PecaResponse.from(pecaRepository.save(peca)));
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
    @Transactional
    public ResponseEntity<PecaResponse> movimentarEstoque(
            @Parameter(description = "ID da peça") @PathVariable UUID id,
            @Valid @RequestBody MovimentacaoEstoqueRequest request) {
        var peca = findById(id);

        if (request.operacao() == MovimentacaoEstoqueRequest.Operacao.ENTRADA) {
            peca.devolverEstoque(request.quantidade());
        } else {
            if (!peca.temEstoque(request.quantidade())) {
                throw new BusinessException(
                    "Estoque insuficiente. Disponível: " + peca.getQuantidadeEstoque()
                    + ", solicitado: " + request.quantidade()
                );
            }
            peca.reservarEstoque(request.quantidade());
        }

        return ResponseEntity.ok(PecaResponse.from(pecaRepository.save(peca)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar peça (soft delete)", description = "Marca a peça como inativa. Peças desativadas não podem ser adicionadas a novas OS.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Peça desativada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada")
    })
    @Transactional
    public ResponseEntity<Void> desativar(@Parameter(description = "ID da peça") @PathVariable UUID id) {
        var peca = findById(id);
        peca.setAtivo(false);
        pecaRepository.save(peca);
        return ResponseEntity.noContent().build();
    }

    private Peca findById(UUID id) {
        return pecaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + id));
    }
}
