package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.ServicoInsumoRequest;
import br.com.fiap.siase.application.dto.input.ServicoRequest;
import br.com.fiap.siase.application.dto.output.ServicoResponse;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Servico;
import br.com.fiap.siase.domain.model.ServicoInsumo;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;
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
@RequestMapping("/servicos")
@Tag(name = "Serviços", description = "Catálogo de serviços da oficina com seus insumos necessários")
public class ServicoController {

    private final ServicoRepositoryPort servicoRepository;
    private final PecaRepositoryPort pecaRepository;

    public ServicoController(ServicoRepositoryPort servicoRepository,
                             PecaRepositoryPort pecaRepository) {
        this.servicoRepository = servicoRepository;
        this.pecaRepository = pecaRepository;
    }

    @GetMapping
    @Operation(summary = "Listar serviços ativos", description = "Retorna apenas os serviços com status ativo.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ServicoResponse>> listar() {
        return ResponseEntity.ok(
                servicoRepository.findByAtivoTrue().stream()
                        .map(ServicoResponse::from)
                        .toList()
        );
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos os serviços", description = "Retorna todos os serviços, incluindo os desativados.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ServicoResponse>> listarTodos() {
        return ResponseEntity.ok(
                servicoRepository.findAll().stream()
                        .map(ServicoResponse::from)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar serviço por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    @Transactional(readOnly = true)
    public ResponseEntity<ServicoResponse> buscar(@Parameter(description = "ID do serviço") @PathVariable UUID id) {
        return ResponseEntity.ok(ServicoResponse.from(findById(id)));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo serviço", description = "Cria um novo serviço no catálogo com preço e tempo estimado de execução.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @Transactional
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request) {
        var servico = new Servico();
        servico.setNome(request.nome());
        servico.setDescricao(request.descricao());
        servico.setPreco(request.preco());
        servico.setTempoEstimadoMinutos(request.tempoEstimadoMinutos());
        var criado = ServicoResponse.from(servicoRepository.save(servico));
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
    @Transactional
    public ResponseEntity<ServicoResponse> atualizar(
            @Parameter(description = "ID do serviço") @PathVariable UUID id,
            @Valid @RequestBody ServicoRequest request) {
        var servico = findById(id);
        servico.setNome(request.nome());
        servico.setDescricao(request.descricao());
        servico.setPreco(request.preco());
        servico.setTempoEstimadoMinutos(request.tempoEstimadoMinutos());
        return ResponseEntity.ok(ServicoResponse.from(servicoRepository.save(servico)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar serviço (soft delete)", description = "Marca o serviço como inativo. Serviços desativados não podem ser adicionados a novas OS.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Serviço desativado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    @Transactional
    public ResponseEntity<Void> desativar(@Parameter(description = "ID do serviço") @PathVariable UUID id) {
        var servico = findById(id);
        servico.setAtivo(false);
        servicoRepository.save(servico);
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
    @Transactional
    public ResponseEntity<ServicoResponse> adicionarInsumo(
            @Parameter(description = "ID do serviço") @PathVariable UUID id,
            @Valid @RequestBody ServicoInsumoRequest request) {
        var servico = findById(id);
        var peca = pecaRepository.findById(request.pecaId())
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + request.pecaId()));

        boolean jaExiste = servico.getInsumos().stream()
                .anyMatch(i -> i.getPeca().getId().equals(request.pecaId()));
        if (jaExiste) {
            throw new BusinessException("Peça '" + peca.getNome()
                    + "' já está vinculada a este serviço. Use PUT para atualizar a quantidade.");
        }

        servico.getInsumos().add(new ServicoInsumo(servico, peca, request.quantidade()));
        return ResponseEntity.ok(ServicoResponse.from(servicoRepository.save(servico)));
    }

    @PutMapping("/{id}/insumos/{pecaId}")
    @Operation(summary = "Atualizar quantidade de insumo", description = "Altera a quantidade necessária de uma peça já vinculada ao serviço.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantidade atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Serviço, peça ou vínculo não encontrado")
    })
    @Transactional
    public ResponseEntity<ServicoResponse> atualizarInsumo(
            @Parameter(description = "ID do serviço") @PathVariable UUID id,
            @Parameter(description = "ID da peça") @PathVariable UUID pecaId,
            @Valid @RequestBody ServicoInsumoRequest request) {
        var servico = findById(id);

        var insumo = servico.getInsumos().stream()
                .filter(i -> i.getPeca().getId().equals(pecaId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Peça não vinculada a este serviço: " + pecaId));

        insumo.setQuantidade(request.quantidade());
        return ResponseEntity.ok(ServicoResponse.from(servicoRepository.save(servico)));
    }

    @DeleteMapping("/{id}/insumos/{pecaId}")
    @Operation(summary = "Remover insumo do serviço", description = "Desvincula a peça do serviço.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Insumo removido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Serviço, peça ou vínculo não encontrado")
    })
    @Transactional
    public ResponseEntity<ServicoResponse> removerInsumo(
            @Parameter(description = "ID do serviço") @PathVariable UUID id,
            @Parameter(description = "ID da peça") @PathVariable UUID pecaId) {
        var servico = findById(id);

        boolean removido = servico.getInsumos()
                .removeIf(i -> i.getPeca().getId().equals(pecaId));

        if (!removido) {
            throw new ResourceNotFoundException("Peça não vinculada a este serviço: " + pecaId);
        }

        return ResponseEntity.ok(ServicoResponse.from(servicoRepository.save(servico)));
    }

    private Servico findById(UUID id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + id));
    }
}
