package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.PagamentoRequest;
import br.com.fiap.siase.dto.response.PagamentoResponse;
import br.com.fiap.siase.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService service;

    @PostMapping("/ordens/{osId}/pagamento")
    public ResponseEntity<PagamentoResponse> registrar(
            @PathVariable UUID osId,
            @Valid @RequestBody PagamentoRequest request) {
        PagamentoResponse response = service.registrar(osId, request);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/pagamentos/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/ordens/{osId}/pagamento")
    public ResponseEntity<PagamentoResponse> buscarPorOS(@PathVariable UUID osId) {
        return ResponseEntity.ok(service.buscarPorOS(osId));
    }

    @PatchMapping("/pagamentos/{id}/confirmar")
    public ResponseEntity<PagamentoResponse> confirmar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.confirmar(id));
    }

    @PatchMapping("/pagamentos/{id}/cancelar")
    public ResponseEntity<PagamentoResponse> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelar(id));
    }
}
