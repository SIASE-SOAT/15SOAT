package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.ItemPecaRequest;
import br.com.fiap.siase.dto.request.ItemServicoRequest;
import br.com.fiap.siase.dto.request.OrdemDeServicoRequest;
import br.com.fiap.siase.dto.response.OrdemDeServicoResponse;
import br.com.fiap.siase.dto.response.PreparacaoAberturaOrdemResponse;
import br.com.fiap.siase.model.enums.StatusOS;
import br.com.fiap.siase.service.OrdemDeServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
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

    @GetMapping("/preparar-abertura")
    public ResponseEntity<PreparacaoAberturaOrdemResponse> prepararAbertura(
            @RequestParam String documento,
            @RequestParam(required = false) String placa) {
        return ResponseEntity.ok(service.prepararAbertura(documento, placa));
    }

    @PostMapping("/{id}/items-peca")
    public ResponseEntity<OrdemDeServicoResponse> adicionarPeca(
            @PathVariable UUID id,
            @Valid @RequestBody ItemPecaRequest request) {
        OrdemDeServicoResponse response = service.adicionarPecaAOrdem(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/items-servico")
    public ResponseEntity<OrdemDeServicoResponse> adicionarServico(
            @PathVariable UUID id,
            @Valid @RequestBody ItemServicoRequest request) {
        OrdemDeServicoResponse response = service.adicionarServicoAOrdem(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/itens-servico/{itemId}/iniciar")
    public ResponseEntity<OrdemDeServicoResponse> iniciarExecucaoItemServico(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(service.iniciarExecucaoItemServico(id, itemId));
    }

    @PatchMapping("/{id}/itens-servico/{itemId}/finalizar")
    public ResponseEntity<OrdemDeServicoResponse> finalizarExecucaoItemServico(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(service.finalizarExecucaoItemServico(id, itemId));
    }

    @PatchMapping("/{id}/avancar")
    public ResponseEntity<OrdemDeServicoResponse> avancarStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(service.avancarStatus(id));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<OrdemDeServicoResponse> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelar(id));
    }

    @GetMapping("/monitoramento/tempo-medio")
    public ResponseEntity<Map<String, Object>> tempoMedioExecucao() {
        double minutos = service.calcularTempoMedioExecucaoMinutos();
        return ResponseEntity.ok(Map.of(
                "tempoMedioMinutos", Math.round(minutos * 100.0) / 100.0,
                "tempoMedioHoras", Math.round((minutos / 60.0) * 100.0) / 100.0,
            "descricao", "Tempo médio de execução dos serviços finalizados"
        ));
    }

    @GetMapping("/acompanhar/{numero}")
    public ResponseEntity<OrdemDeServicoResponse> acompanhar(@PathVariable String numero) {
        return ResponseEntity.ok(service.buscarPorNumero(numero));
    }
}
