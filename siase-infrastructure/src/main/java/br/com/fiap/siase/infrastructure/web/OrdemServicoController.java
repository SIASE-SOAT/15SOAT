package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.ItemPecaRequest;
import br.com.fiap.siase.application.dto.input.ItemServicoRequest;
import br.com.fiap.siase.application.dto.input.OrdemDeServicoRequest;
import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.dto.output.PreparacaoAberturaOrdemResponse;
import br.com.fiap.siase.application.usecase.AprovarOrcamentoUC;
import br.com.fiap.siase.application.usecase.ConsultarStatusOSUC;
import br.com.fiap.siase.application.usecase.CriarOrdemServicoUC;
import br.com.fiap.siase.application.usecase.ListarOrdensServicoUC;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.ItemPeca;
import br.com.fiap.siase.domain.model.ItemServico;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
import br.com.fiap.siase.infrastructure.persistence.OrdemServicoJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.UUID;

@RestController
@RequestMapping("/ordens")
@Tag(name = "Ordens de Serviço")
public class OrdemServicoController {

    private final CriarOrdemServicoUC criarOrdemServicoUC;
    private final ListarOrdensServicoUC listarOrdensServicoUC;
    private final ConsultarStatusOSUC consultarStatusOSUC;
    private final AprovarOrcamentoUC aprovarOrcamentoUC;
    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final OrdemServicoJpaRepository ordemServicoJpaRepository;
    private final ClienteRepositoryPort clienteRepository;
    private final VeiculoRepositoryPort veiculoRepository;
    private final PecaRepositoryPort pecaRepository;
    private final ServicoRepositoryPort servicoRepository;

    public OrdemServicoController(
            CriarOrdemServicoUC criarOrdemServicoUC,
            ListarOrdensServicoUC listarOrdensServicoUC,
            ConsultarStatusOSUC consultarStatusOSUC,
            AprovarOrcamentoUC aprovarOrcamentoUC,
            OrdemServicoRepositoryPort ordemServicoRepository,
            OrdemServicoJpaRepository ordemServicoJpaRepository,
            ClienteRepositoryPort clienteRepository,
            VeiculoRepositoryPort veiculoRepository,
            PecaRepositoryPort pecaRepository,
            ServicoRepositoryPort servicoRepository) {
        this.criarOrdemServicoUC = criarOrdemServicoUC;
        this.listarOrdensServicoUC = listarOrdensServicoUC;
        this.consultarStatusOSUC = consultarStatusOSUC;
        this.aprovarOrcamentoUC = aprovarOrcamentoUC;
        this.ordemServicoRepository = ordemServicoRepository;
        this.ordemServicoJpaRepository = ordemServicoJpaRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.pecaRepository = pecaRepository;
        this.servicoRepository = servicoRepository;
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
    @Operation(summary = "Listar ordens de serviço ativas ordenadas")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<List<OrdemDeServicoResponse>> listar() {
        return ResponseEntity.ok(listarOrdensServicoUC.executar());
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
        OrdemDeServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + id));

        try {
            os.avancarStatus();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        OrdemDeServico salvo = ordemServicoRepository.save(os);
        return ResponseEntity.ok(OrdemDeServicoResponse.from(salvo));
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
        OrdemDeServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + id));

        try {
            os.cancelar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        OrdemDeServico salvo = ordemServicoRepository.save(os);
        return ResponseEntity.ok(OrdemDeServicoResponse.from(salvo));
    }

    @GetMapping("/monitoramento/tempo-medio")
    @Operation(summary = "Tempo médio de execução")
    @ApiResponse(responseCode = "200", description = "Tempo médio calculado")
    public ResponseEntity<Map<String, Object>> tempoMedioExecucao() {
        Double resultado = ordemServicoJpaRepository.calcularTempoMedioExecucaoMinutos();
        double minutos = resultado != null ? resultado : 0.0;
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
        OrdemDeServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + id));

        var peca = pecaRepository.findByIdParaAtualizacao(request.pecaId())
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + request.pecaId()));

        if (!Boolean.TRUE.equals(peca.getAtivo())) {
            throw new BusinessException("Peça desativada não pode ser adicionada: " + request.pecaId());
        }

        boolean jaExiste = os.getItensPeca().stream()
                .anyMatch(item -> item.getPeca().getId().equals(request.pecaId()));
        if (jaExiste) {
            throw new BusinessException("Esta peça já foi adicionada a esta ordem de serviço.");
        }

        try {
            peca.reservarEstoque(request.quantidade());
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        var item = new ItemPeca();
        item.setOrdemDeServico(os);
        item.setPeca(peca);
        item.setQuantidade(request.quantidade());
        item.setPrecoUnitario(peca.getPreco());
        os.getItensPeca().add(item);
        os.recalcularTotais();

        return ResponseEntity.ok(OrdemDeServicoResponse.from(ordemServicoRepository.save(os)));
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
        OrdemDeServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + id));

        var servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + request.servicoId()));

        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new BusinessException("Serviço desativado não pode ser adicionado: " + request.servicoId());
        }

        boolean jaExiste = os.getItensServico().stream()
                .anyMatch(item -> item.getServico().getId().equals(request.servicoId()));
        if (jaExiste) {
            throw new BusinessException("Este serviço já foi adicionado a esta ordem de serviço.");
        }

        var item = new ItemServico();
        item.setOrdemDeServico(os);
        item.setServico(servico);
        item.setPrecoUnitario(servico.getPreco());
        item.setTempoEstimadoMinutos(servico.getTempoEstimadoMinutos());
        item.setObservacoes(request.observacoes());
        os.getItensServico().add(item);
        os.recalcularTotais();

        return ResponseEntity.ok(OrdemDeServicoResponse.from(ordemServicoRepository.save(os)));
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
        String documentoLimpo = documento.replaceAll("\\D", "");

        var cliente = clienteRepository.findByDocumento(documentoLimpo)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o documento: " + documento));

        var veiculosAtivos = veiculoRepository.findByClienteId(cliente.getId()).stream()
                .filter(veiculo -> Boolean.TRUE.equals(veiculo.getAtivo()))
                .map(PreparacaoAberturaOrdemResponse.VeiculoIdentificadoResponse::from)
                .toList();

        PreparacaoAberturaOrdemResponse.VeiculoIdentificadoResponse veiculoSelecionado = null;
        if (placa != null && !placa.isBlank()) {
            String placaNormalizada = placa.toUpperCase().trim();
            var veiculo = veiculoRepository.findByPlaca(placaNormalizada)
                    .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado para a placa: " + placa));

            if (!veiculo.getCliente().getId().equals(cliente.getId())) {
                throw new BusinessException("O veículo informado não pertence ao cliente identificado pelo documento.");
            }

            if (!Boolean.TRUE.equals(veiculo.getAtivo())) {
                throw new BusinessException("O veículo informado está inativo e não pode ser usado para abrir uma OS.");
            }

            veiculoSelecionado = PreparacaoAberturaOrdemResponse.VeiculoIdentificadoResponse.from(veiculo);
        }

        return ResponseEntity.ok(new PreparacaoAberturaOrdemResponse(
                PreparacaoAberturaOrdemResponse.ClienteIdentificadoResponse.from(cliente),
                veiculosAtivos,
                veiculoSelecionado,
                veiculoSelecionado != null
        ));
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
        OrdemDeServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + id));

        if (os.getStatus() != br.com.fiap.siase.domain.enums.StatusOS.EM_EXECUCAO) {
            throw new BusinessException("Só é possível iniciar a execução de serviços quando a OS está em execução.");
        }

        ItemServico item = os.getItensServico().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item de serviço não encontrado: " + itemId));

        item.iniciarExecucao();
        return ResponseEntity.ok(OrdemDeServicoResponse.from(ordemServicoRepository.save(os)));
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
        OrdemDeServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + id));

        if (os.getStatus() != br.com.fiap.siase.domain.enums.StatusOS.EM_EXECUCAO) {
            throw new BusinessException("Só é possível finalizar a execução de serviços quando a OS está em execução.");
        }

        ItemServico item = os.getItensServico().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item de serviço não encontrado: " + itemId));

        item.finalizarExecucao();
        return ResponseEntity.ok(OrdemDeServicoResponse.from(ordemServicoRepository.save(os)));
    }
}
