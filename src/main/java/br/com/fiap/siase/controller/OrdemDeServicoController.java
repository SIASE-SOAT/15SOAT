package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.ItemPecaRequest;
import br.com.fiap.siase.dto.request.ItemServicoRequest;
import br.com.fiap.siase.dto.request.OrdemDeServicoRequest;
import br.com.fiap.siase.dto.response.OrdemDeServicoResponse;
import br.com.fiap.siase.dto.response.PreparacaoAberturaOrdemResponse;
import br.com.fiap.siase.model.enums.StatusOS;
import br.com.fiap.siase.service.OrdemDeServicoService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ordens")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço", description = "Ciclo de vida completo das ordens de serviço: abertura, execução, finalização e entrega")
public class OrdemDeServicoController {

    private final OrdemDeServicoService service;

    @PostMapping
    @Operation(
        summary = "Abrir ordem de serviço",
        description = "Cria uma nova OS com status RECEBIDA. Requer cliente, veículo e ao menos um serviço. Peças são opcionais e têm estoque reservado no momento da criação."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OS criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Cliente, veículo, serviço ou peça não encontrados"),
        @ApiResponse(responseCode = "422", description = "Veículo já possui OS em andamento ou estoque insuficiente")
    })
    public ResponseEntity<OrdemDeServicoResponse> criar(@Valid @RequestBody OrdemDeServicoRequest request) {
        OrdemDeServicoResponse response = service.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(
        summary = "Listar ordens de serviço",
        description = "Retorna todas as OS ou filtra pelo status informado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<List<OrdemDeServicoResponse>> listar(
            @Parameter(description = "Filtro opcional por status (RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, APROVADO, EM_EXECUCAO, FINALIZADA, ENTREGUE, CANCELADA)")
            @RequestParam(required = false) StatusOS status) {
        List<OrdemDeServicoResponse> lista = status != null
                ? service.listarPorStatus(status)
                : service.listar();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar OS por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OS encontrada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada")
    })
    public ResponseEntity<OrdemDeServicoResponse> buscarPorId(
            @Parameter(description = "ID da OS") @PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/preparar-abertura")
    @Operation(
        summary = "Preparar abertura de OS",
        description = "Identifica o cliente pelo CPF/CNPJ e seus veículos ativos. Se a placa for informada, valida o vínculo e retorna pronto para abertura."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dados retornados"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "422", description = "Veículo pertence a outro cliente")
    })
    public ResponseEntity<PreparacaoAberturaOrdemResponse> prepararAbertura(
            @Parameter(description = "CPF ou CNPJ do cliente (com ou sem máscara)") @RequestParam String documento,
            @Parameter(description = "Placa do veículo (opcional)") @RequestParam(required = false) String placa) {
        return ResponseEntity.ok(service.prepararAbertura(documento, placa));
    }

    @PostMapping("/{id}/items-peca")
    @Operation(
        summary = "Adicionar peça à OS",
        description = "Vincula uma peça à OS e reserva estoque. Permitido nos status RECEBIDA, EM_DIAGNOSTICO e EM_EXECUCAO."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Peça adicionada e totais recalculados"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS ou peça não encontradas"),
        @ApiResponse(responseCode = "422", description = "Status inválido, peça desativada, estoque insuficiente ou peça duplicada")
    })
    public ResponseEntity<OrdemDeServicoResponse> adicionarPeca(
            @Parameter(description = "ID da OS") @PathVariable UUID id,
            @Valid @RequestBody ItemPecaRequest request) {
        return ResponseEntity.ok(service.adicionarPecaAOrdem(id, request));
    }

    @PostMapping("/{id}/items-servico")
    @Operation(
        summary = "Adicionar serviço à OS",
        description = "Vincula um serviço à OS. Permitido nos status RECEBIDA, EM_DIAGNOSTICO e EM_EXECUCAO."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serviço adicionado e totais recalculados"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS ou serviço não encontrados"),
        @ApiResponse(responseCode = "422", description = "Status inválido, serviço desativado ou duplicado")
    })
    public ResponseEntity<OrdemDeServicoResponse> adicionarServico(
            @Parameter(description = "ID da OS") @PathVariable UUID id,
            @Valid @RequestBody ItemServicoRequest request) {
        return ResponseEntity.ok(service.adicionarServicoAOrdem(id, request));
    }

    @PatchMapping("/{id}/itens-servico/{itemId}/iniciar")
    @Operation(
        summary = "Iniciar execução de item de serviço",
        description = "Registra o início da execução de um serviço específico dentro da OS (status EM_EXECUCAO)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Execução iniciada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS ou item não encontrados"),
        @ApiResponse(responseCode = "422", description = "OS não está em execução")
    })
    public ResponseEntity<OrdemDeServicoResponse> iniciarExecucaoItemServico(
            @Parameter(description = "ID da OS") @PathVariable UUID id,
            @Parameter(description = "ID do item de serviço") @PathVariable UUID itemId) {
        return ResponseEntity.ok(service.iniciarExecucaoItemServico(id, itemId));
    }

    @PatchMapping("/{id}/itens-servico/{itemId}/finalizar")
    @Operation(
        summary = "Finalizar execução de item de serviço",
        description = "Registra o fim da execução de um serviço específico dentro da OS."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Execução finalizada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS ou item não encontrados"),
        @ApiResponse(responseCode = "422", description = "OS não está em execução")
    })
    public ResponseEntity<OrdemDeServicoResponse> finalizarExecucaoItemServico(
            @Parameter(description = "ID da OS") @PathVariable UUID id,
            @Parameter(description = "ID do item de serviço") @PathVariable UUID itemId) {
        return ResponseEntity.ok(service.finalizarExecucaoItemServico(id, itemId));
    }

    @PatchMapping("/{id}/avancar")
    @Operation(
        summary = "Avançar status da OS",
        description = "Avança a OS para o próximo status no fluxo: RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → APROVADO → EM_EXECUCAO → FINALIZADA → ENTREGUE."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status avançado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "422", description = "OS já entregue ou cancelada")
    })
    public ResponseEntity<OrdemDeServicoResponse> avancarStatus(
            @Parameter(description = "ID da OS") @PathVariable UUID id) {
        return ResponseEntity.ok(service.avancarStatus(id));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(
        summary = "Cancelar OS",
        description = "Cancela a OS. Permitido nos status RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO e APROVADO."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OS cancelada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "422", description = "OS em status que não permite cancelamento")
    })
    public ResponseEntity<OrdemDeServicoResponse> cancelar(
            @Parameter(description = "ID da OS") @PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelar(id));
    }

    @GetMapping("/monitoramento/tempo-medio")
    @Operation(
        summary = "Tempo médio de execução",
        description = "Calcula o tempo médio de execução das OS finalizadas, em minutos e horas."
    )
    @ApiResponse(responseCode = "200", description = "Tempo médio calculado")
    public ResponseEntity<Map<String, Object>> tempoMedioExecucao() {
        double minutos = service.calcularTempoMedioExecucaoMinutos();
        return ResponseEntity.ok(Map.of(
                "tempoMedioMinutos", Math.round(minutos * 100.0) / 100.0,
                "tempoMedioHoras", Math.round((minutos / 60.0) * 100.0) / 100.0,
                "descricao", "Tempo médio de execução dos serviços finalizados"
        ));
    }

    @GetMapping("/acompanhar/{numero}")
    @Operation(
        summary = "Acompanhar OS por número (público)",
        description = "Endpoint público — não requer autenticação. Permite ao cliente acompanhar o status da OS pelo número impresso no recibo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OS encontrada"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada")
    })
    public ResponseEntity<OrdemDeServicoResponse> acompanhar(
            @Parameter(description = "Número da OS (ex: OS-20260412-ABCDEF)") @PathVariable String numero) {
        return ResponseEntity.ok(service.buscarPorNumero(numero));
    }

    @PatchMapping("/acompanhar/{numero}/aprovar-orcamento")
    @Operation(
        summary = "Aprovar orçamento (público)",
        description = "Endpoint público — não requer autenticação. O cliente aprova o orçamento, avançando a OS de AGUARDANDO_APROVACAO para APROVADO."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orçamento aprovado"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "422", description = "OS não está aguardando aprovação")
    })
    public ResponseEntity<OrdemDeServicoResponse> aprovarOrcamentoPublico(
            @Parameter(description = "Número da OS") @PathVariable String numero) {
        return ResponseEntity.ok(service.aprovarOrcamentoPorNumero(numero));
    }

    @PatchMapping("/acompanhar/{numero}/recusar-orcamento")
    @Operation(
        summary = "Recusar orçamento (público)",
        description = "Endpoint público — não requer autenticação. O cliente recusa o orçamento, cancelando a OS."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orçamento recusado e OS cancelada"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "422", description = "OS não está aguardando aprovação")
    })
    public ResponseEntity<OrdemDeServicoResponse> recusarOrcamentoPublico(
            @Parameter(description = "Número da OS") @PathVariable String numero) {
        return ResponseEntity.ok(service.recusarOrcamentoPorNumero(numero));
    }
}
