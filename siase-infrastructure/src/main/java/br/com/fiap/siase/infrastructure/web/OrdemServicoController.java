package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.ItemPecaRequest;
import br.com.fiap.siase.application.dto.input.ItemServicoRequest;
import br.com.fiap.siase.application.dto.input.OrdemDeServicoRequest;
import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.dto.output.PreparacaoAberturaOrdemResponse;
import br.com.fiap.siase.application.usecase.port.AdicionarPecaUCPort;
import br.com.fiap.siase.application.usecase.port.AdicionarServicoUCPort;
import br.com.fiap.siase.application.usecase.port.AprovarOrcamentoUCPort;
import br.com.fiap.siase.application.usecase.port.AvancarStatusUCPort;
import br.com.fiap.siase.application.usecase.port.CancelarOrdemUCPort;
import br.com.fiap.siase.application.usecase.port.ConsultarStatusOSUCPort;
import br.com.fiap.siase.application.usecase.port.ConsultarTempoMedioUCPort;
import br.com.fiap.siase.application.usecase.port.CriarOrdemServicoUCPort;
import br.com.fiap.siase.application.usecase.port.FinalizarExecucaoItemUCPort;
import br.com.fiap.siase.application.usecase.port.IniciarExecucaoItemUCPort;
import br.com.fiap.siase.application.usecase.port.ListarOrdensServicoUCPort;
import br.com.fiap.siase.application.usecase.port.PrepararAberturaOSUCPort;
import br.com.fiap.siase.domain.enums.StatusOS;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/ordens")
@Tag(name = "Ordens de Serviço")
public class OrdemServicoController {

    private final CriarOrdemServicoUCPort criarOrdemServicoUC;
    private final ListarOrdensServicoUCPort listarOrdensServicoUC;
    private final ConsultarStatusOSUCPort consultarStatusOSUC;
    private final AprovarOrcamentoUCPort aprovarOrcamentoUC;
    private final AvancarStatusUCPort avancarStatusUC;
    private final CancelarOrdemUCPort cancelarOrdemUC;
    private final AdicionarPecaUCPort adicionarPecaUC;
    private final AdicionarServicoUCPort adicionarServicoUC;
    private final ConsultarTempoMedioUCPort consultarTempoMedioUC;
    private final PrepararAberturaOSUCPort prepararAberturaOSUC;
    private final IniciarExecucaoItemUCPort iniciarExecucaoItemUC;
    private final FinalizarExecucaoItemUCPort finalizarExecucaoItemUC;

    public OrdemServicoController(
            CriarOrdemServicoUCPort criarOrdemServicoUC,
            ListarOrdensServicoUCPort listarOrdensServicoUC,
            ConsultarStatusOSUCPort consultarStatusOSUC,
            AprovarOrcamentoUCPort aprovarOrcamentoUC,
            AvancarStatusUCPort avancarStatusUC,
            CancelarOrdemUCPort cancelarOrdemUC,
            AdicionarPecaUCPort adicionarPecaUC,
            AdicionarServicoUCPort adicionarServicoUC,
            ConsultarTempoMedioUCPort consultarTempoMedioUC,
            PrepararAberturaOSUCPort prepararAberturaOSUC,
            IniciarExecucaoItemUCPort iniciarExecucaoItemUC,
            FinalizarExecucaoItemUCPort finalizarExecucaoItemUC) {
        this.criarOrdemServicoUC = criarOrdemServicoUC;
        this.listarOrdensServicoUC = listarOrdensServicoUC;
        this.consultarStatusOSUC = consultarStatusOSUC;
        this.aprovarOrcamentoUC = aprovarOrcamentoUC;
        this.avancarStatusUC = avancarStatusUC;
        this.cancelarOrdemUC = cancelarOrdemUC;
        this.adicionarPecaUC = adicionarPecaUC;
        this.adicionarServicoUC = adicionarServicoUC;
        this.consultarTempoMedioUC = consultarTempoMedioUC;
        this.prepararAberturaOSUC = prepararAberturaOSUC;
        this.iniciarExecucaoItemUC = iniciarExecucaoItemUC;
        this.finalizarExecucaoItemUC = finalizarExecucaoItemUC;
    }

    @PostMapping
    @Operation(summary = "Criar ordem de serviço")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OS criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Cliente, veículo, serviço ou peça não encontrados"),
        @ApiResponse(responseCode = "422", description = "Veículo já possui OS em andamento ou estoque insuficiente")
    })
    public ResponseEntity<OrdemDeServicoResponse> criar(@Valid @RequestBody OrdemDeServicoRequest request) {
        OrdemDeServicoResponse response = criarOrdemServicoUC.executar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar ordens de serviço")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<List<OrdemDeServicoResponse>> listar(
            @Parameter(description = "Filtro opcional por status (RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, APROVADO, EM_EXECUCAO, FINALIZADA, ENTREGUE, CANCELADA)")
            @RequestParam(required = false) StatusOS status) {
        return ResponseEntity.ok(listarOrdensServicoUC.executar(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar OS por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OS encontrada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada")
    })
    public ResponseEntity<OrdemDeServicoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(consultarStatusOSUC.executar(id));
    }

    @GetMapping("/acompanhar/{numero}")
    @Operation(summary = "Acompanhar OS por número (público)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OS encontrada"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada")
    })
    public ResponseEntity<OrdemDeServicoResponse> acompanhar(@PathVariable String numero) {
        return ResponseEntity.ok(consultarStatusOSUC.executarPorNumero(numero));
    }

    @PatchMapping("/acompanhar/{numero}/aprovar-orcamento")
    @Operation(summary = "Aprovar orçamento (público)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orçamento aprovado"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "422", description = "OS não está aguardando aprovação")
    })
    public ResponseEntity<OrdemDeServicoResponse> aprovarOrcamentoPublico(@PathVariable String numero) {
        return ResponseEntity.ok(aprovarOrcamentoUC.aprovar(numero));
    }

    @PatchMapping("/acompanhar/{numero}/recusar-orcamento")
    @Operation(summary = "Recusar orçamento (público)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orçamento recusado e OS cancelada"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "422", description = "OS não está aguardando aprovação")
    })
    public ResponseEntity<OrdemDeServicoResponse> recusarOrcamentoPublico(@PathVariable String numero) {
        return ResponseEntity.ok(aprovarOrcamentoUC.recusar(numero));
    }

    @PatchMapping("/{id}/avancar")
    @Operation(summary = "Avançar status da OS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status avançado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "422", description = "OS já entregue ou cancelada")
    })
    public ResponseEntity<OrdemDeServicoResponse> avancar(@PathVariable UUID id) {
        return ResponseEntity.ok(avancarStatusUC.executar(id));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar OS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OS cancelada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "422", description = "OS em status que não permite cancelamento")
    })
    public ResponseEntity<OrdemDeServicoResponse> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(cancelarOrdemUC.executar(id));
    }

    @GetMapping("/monitoramento/tempo-medio")
    @Operation(summary = "Tempo médio de execução")
    @ApiResponse(responseCode = "200", description = "Tempo médio calculado")
    public ResponseEntity<Map<String, Object>> tempoMedioExecucao() {
        double minutos = Optional.ofNullable(consultarTempoMedioUC.executar()).orElse(0.0);
        return ResponseEntity.ok(Map.of(
                "tempoMedioMinutos", Math.round(minutos * 100.0) / 100.0,
                "tempoMedioHoras", Math.round((minutos / 60.0) * 100.0) / 100.0,
                "descricao", "Tempo médio de execução dos serviços finalizados"
        ));
    }

    @PostMapping("/{id}/items-peca")
    @Operation(summary = "Adicionar peça à OS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Peça adicionada e totais recalculados"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS ou peça não encontradas"),
        @ApiResponse(responseCode = "422", description = "Status inválido, peça desativada, estoque insuficiente ou peça duplicada")
    })
    public ResponseEntity<OrdemDeServicoResponse> adicionarPeca(
            @PathVariable UUID id,
            @Valid @RequestBody ItemPecaRequest request) {
        return ResponseEntity.ok(adicionarPecaUC.executar(id, request));
    }

    @PostMapping("/{id}/items-servico")
    @Operation(summary = "Adicionar serviço à OS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serviço adicionado e totais recalculados"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS ou serviço não encontrados"),
        @ApiResponse(responseCode = "422", description = "Status inválido, serviço desativado ou duplicado")
    })
    public ResponseEntity<OrdemDeServicoResponse> adicionarServico(
            @PathVariable UUID id,
            @Valid @RequestBody ItemServicoRequest request) {
        return ResponseEntity.ok(adicionarServicoUC.executar(id, request));
    }

    @GetMapping("/preparar-abertura")
    @Operation(summary = "Preparar abertura de OS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dados retornados"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "422", description = "Veículo pertence a outro cliente")
    })
    public ResponseEntity<PreparacaoAberturaOrdemResponse> prepararAbertura(
            @RequestParam String documento,
            @RequestParam(required = false) String placa) {
        return ResponseEntity.ok(prepararAberturaOSUC.executar(documento, placa));
    }

    @PatchMapping("/{id}/itens-servico/{itemId}/iniciar")
    @Operation(summary = "Iniciar execução de item de serviço")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Execução iniciada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS ou item não encontrados"),
        @ApiResponse(responseCode = "422", description = "OS não está em execução")
    })
    public ResponseEntity<OrdemDeServicoResponse> iniciarExecucao(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(iniciarExecucaoItemUC.executar(id, itemId));
    }

    @PatchMapping("/{id}/itens-servico/{itemId}/finalizar")
    @Operation(summary = "Finalizar execução de item de serviço")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Execução finalizada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS ou item não encontrados"),
        @ApiResponse(responseCode = "422", description = "OS não está em execução")
    })
    public ResponseEntity<OrdemDeServicoResponse> finalizarExecucao(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(finalizarExecucaoItemUC.executar(id, itemId));
    }
}
