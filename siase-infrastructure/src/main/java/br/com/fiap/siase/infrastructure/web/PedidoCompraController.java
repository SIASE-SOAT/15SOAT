package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.PedidoCompraRequest;
import br.com.fiap.siase.application.dto.output.PedidoCompraResponse;
import br.com.fiap.siase.domain.enums.StatusPedidoCompra;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.PedidoCompra;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import br.com.fiap.siase.domain.port.PedidoCompraRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos-compra")
@Tag(name = "Pedidos de Compra", description = "Gerenciamento de pedidos de reposição de estoque")
public class PedidoCompraController {

    private final PedidoCompraRepositoryPort pedidoCompraRepository;
    private final PecaRepositoryPort pecaRepository;

    public PedidoCompraController(PedidoCompraRepositoryPort pedidoCompraRepository,
                                  PecaRepositoryPort pecaRepository) {
        this.pedidoCompraRepository = pedidoCompraRepository;
        this.pecaRepository = pecaRepository;
    }

    @PostMapping
    @Operation(
        summary = "Criar pedido de compra",
        description = "Abre um pedido de compra PENDENTE para reposição de uma peça."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada")
    })
    @Transactional
    public ResponseEntity<PedidoCompraResponse> criar(@Valid @RequestBody PedidoCompraRequest request) {
        var peca = pecaRepository.findById(request.pecaId())
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + request.pecaId()));

        var pedido = new PedidoCompra();
        pedido.setPeca(peca);
        pedido.setQuantidadeSolicitada(request.quantidadeSolicitada());
        pedido.setObservacoes(request.observacoes());

        PedidoCompraResponse response = PedidoCompraResponse.from(pedidoCompraRepository.save(pedido));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(
        summary = "Listar pedidos de compra",
        description = "Retorna todos os pedidos ou filtra pelo status informado (PENDENTE, APROVADO, RECEBIDO, CANCELADO)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @Transactional(readOnly = true)
    public ResponseEntity<List<PedidoCompraResponse>> listar(
            @Parameter(description = "Filtro opcional por status") @RequestParam(required = false) StatusPedidoCompra status) {
        List<PedidoCompraResponse> lista = status != null
                ? pedidoCompraRepository.findByStatus(status).stream().map(PedidoCompraResponse::from).toList()
                : pedidoCompraRepository.findAll().stream().map(PedidoCompraResponse::from).toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pedido de compra por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @Transactional(readOnly = true)
    public ResponseEntity<PedidoCompraResponse> buscarPorId(
            @Parameter(description = "ID do pedido") @PathVariable UUID id) {
        return ResponseEntity.ok(PedidoCompraResponse.from(findById(id)));
    }

    @PatchMapping("/{id}/aprovar")
    @Operation(
        summary = "Aprovar pedido de compra",
        description = "Muda o status de PENDENTE para APROVADO, liberando o pedido para recebimento."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido aprovado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
        @ApiResponse(responseCode = "422", description = "Apenas pedidos PENDENTES podem ser aprovados")
    })
    @Transactional
    public ResponseEntity<PedidoCompraResponse> aprovar(
            @Parameter(description = "ID do pedido") @PathVariable UUID id) {
        var pedido = findById(id);
        try {
            pedido.aprovar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        return ResponseEntity.ok(PedidoCompraResponse.from(pedidoCompraRepository.save(pedido)));
    }

    @PatchMapping("/{id}/receber")
    @Operation(
        summary = "Receber pedido de compra",
        description = "Registra o recebimento das mercadorias, atualiza o estoque da peça e muda o status para RECEBIDO."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recebimento registrado e estoque atualizado"),
        @ApiResponse(responseCode = "400", description = "Quantidade inválida (mínimo 1)"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
        @ApiResponse(responseCode = "422", description = "Apenas pedidos APROVADOS podem ser recebidos")
    })
    @Transactional
    public ResponseEntity<PedidoCompraResponse> receber(
            @Parameter(description = "ID do pedido") @PathVariable UUID id,
            @Parameter(description = "Quantidade efetivamente recebida (mínimo 1)") @RequestParam @Min(1) int quantidade) {
        var pedido = findById(id);

        if (quantidade <= 0) {
            throw new BusinessException("Quantidade recebida deve ser maior que zero.");
        }

        try {
            pedido.receber(quantidade);
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        pecaRepository.save(pedido.getPeca());
        return ResponseEntity.ok(PedidoCompraResponse.from(pedidoCompraRepository.save(pedido)));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(
        summary = "Cancelar pedido de compra",
        description = "Cancela um pedido PENDENTE ou APROVADO."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido cancelado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
        @ApiResponse(responseCode = "422", description = "Pedidos já RECEBIDOS não podem ser cancelados")
    })
    @Transactional
    public ResponseEntity<PedidoCompraResponse> cancelar(
            @Parameter(description = "ID do pedido") @PathVariable UUID id) {
        var pedido = findById(id);
        try {
            pedido.cancelar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        return ResponseEntity.ok(PedidoCompraResponse.from(pedidoCompraRepository.save(pedido)));
    }

    private PedidoCompra findById(UUID id) {
        return pedidoCompraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de compra não encontrado: " + id));
    }
}
