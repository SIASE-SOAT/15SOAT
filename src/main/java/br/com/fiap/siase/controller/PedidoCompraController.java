package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.PedidoCompraRequest;
import br.com.fiap.siase.dto.response.PedidoCompraResponse;
import br.com.fiap.siase.model.enums.StatusPedidoCompra;
import br.com.fiap.siase.service.PedidoCompraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos-compra")
@RequiredArgsConstructor
@Tag(name = "Pedidos de Compra", description = "Gerenciamento de pedidos de reposição de estoque")
public class PedidoCompraController {

    private final PedidoCompraService service;

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
    public ResponseEntity<PedidoCompraResponse> criar(@Valid @RequestBody PedidoCompraRequest request) {
        PedidoCompraResponse response = service.criar(request);
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
    public ResponseEntity<List<PedidoCompraResponse>> listar(
            @Parameter(description = "Filtro opcional por status") @RequestParam(required = false) StatusPedidoCompra status) {
        List<PedidoCompraResponse> lista = status != null
                ? service.listarPorStatus(status)
                : service.listar();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pedido de compra por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<PedidoCompraResponse> buscarPorId(
            @Parameter(description = "ID do pedido") @PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
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
    public ResponseEntity<PedidoCompraResponse> aprovar(
            @Parameter(description = "ID do pedido") @PathVariable UUID id) {
        return ResponseEntity.ok(service.aprovar(id));
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
    public ResponseEntity<PedidoCompraResponse> receber(
            @Parameter(description = "ID do pedido") @PathVariable UUID id,
            @Parameter(description = "Quantidade efetivamente recebida (mínimo 1)") @RequestParam @Min(1) int quantidade) {
        return ResponseEntity.ok(service.receber(id, quantidade));
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
    public ResponseEntity<PedidoCompraResponse> cancelar(
            @Parameter(description = "ID do pedido") @PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelar(id));
    }
}
