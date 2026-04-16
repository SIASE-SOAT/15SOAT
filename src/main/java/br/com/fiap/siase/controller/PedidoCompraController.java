package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.PedidoCompraRequest;
import br.com.fiap.siase.dto.response.PedidoCompraResponse;
import br.com.fiap.siase.model.enums.StatusPedidoCompra;
import br.com.fiap.siase.service.PedidoCompraService;
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
public class PedidoCompraController {

    private final PedidoCompraService service;

    @PostMapping
    public ResponseEntity<PedidoCompraResponse> criar(@Valid @RequestBody PedidoCompraRequest request) {
        PedidoCompraResponse response = service.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PedidoCompraResponse>> listar(
            @RequestParam(required = false) StatusPedidoCompra status) {
        List<PedidoCompraResponse> lista = status != null
                ? service.listarPorStatus(status)
                : service.listar();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoCompraResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PatchMapping("/{id}/aprovar")
    public ResponseEntity<PedidoCompraResponse> aprovar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.aprovar(id));
    }

    @PatchMapping("/{id}/receber")
    public ResponseEntity<PedidoCompraResponse> receber(
            @PathVariable UUID id,
            @RequestParam @Min(1) int quantidade) {
        return ResponseEntity.ok(service.receber(id, quantidade));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<PedidoCompraResponse> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelar(id));
    }
}
