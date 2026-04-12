package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.OrdemDeServicoRequest;
import br.com.fiap.siase.dto.response.OrdemDeServicoResponse;
import br.com.fiap.siase.model.enums.StatusOS;
import br.com.fiap.siase.service.OrdemDeServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ordens")
@RequiredArgsConstructor
public class OrdemDeServicoController {

    private final OrdemDeServicoService service;

    @PostMapping
    public ResponseEntity<OrdemDeServicoResponse> criar(@Valid @RequestBody OrdemDeServicoRequest request) {
        OrdemDeServicoResponse response = service.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrdemDeServicoResponse>> listar(
            @RequestParam(required = false) StatusOS status) {
        List<OrdemDeServicoResponse> lista = status != null
                ? service.listarPorStatus(status)
                : service.listar();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdemDeServicoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PatchMapping("/{id}/avancar")
    public ResponseEntity<OrdemDeServicoResponse> avancarStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(service.avancarStatus(id));
    }

    @GetMapping("/acompanhar/{numero}")
    public ResponseEntity<OrdemDeServicoResponse> acompanhar(@PathVariable String numero) {
        return ResponseEntity.ok(service.buscarPorNumero(numero));
    }
}
