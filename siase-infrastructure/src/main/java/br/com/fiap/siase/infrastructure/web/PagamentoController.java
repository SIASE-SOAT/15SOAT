package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.PagamentoRequest;
import br.com.fiap.siase.application.dto.output.PagamentoResponse;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Pagamento;
import br.com.fiap.siase.domain.port.EmailPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.PagamentoRepositoryPort;
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

import java.util.UUID;

@RestController
@Tag(name = "Pagamentos", description = "Registro e ciclo de vida de pagamentos vinculados a ordens de serviço")
public class PagamentoController {

    private final PagamentoRepositoryPort pagamentoRepository;
    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final EmailPort emailPort;

    public PagamentoController(PagamentoRepositoryPort pagamentoRepository,
                               OrdemServicoRepositoryPort ordemServicoRepository,
                               EmailPort emailPort) {
        this.pagamentoRepository = pagamentoRepository;
        this.ordemServicoRepository = ordemServicoRepository;
        this.emailPort = emailPort;
    }

    @PostMapping("/ordens/{osId}/pagamento")
    @Operation(
        summary = "Registrar pagamento",
        description = "Cria um pagamento PENDENTE para uma OS no status FINALIZADA. Cada OS admite apenas um pagamento ativo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pagamento registrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "422", description = "OS não está FINALIZADA ou já possui pagamento ativo")
    })
    @Transactional
    public ResponseEntity<PagamentoResponse> registrar(
            @Parameter(description = "ID da ordem de serviço") @PathVariable UUID osId,
            @Valid @RequestBody PagamentoRequest request) {
        var os = ordemServicoRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + osId));

        if (os.getStatus() != StatusOS.FINALIZADA) {
            throw new BusinessException(
                "Pagamento só pode ser registrado para OS com status FINALIZADA. Status atual: "
                + os.getStatus().getDescricao());
        }

        if (pagamentoRepository.existsByOrdemDeServicoId(osId)) {
            throw new BusinessException("Esta OS já possui um pagamento registrado.");
        }

        var pagamento = new Pagamento();
        pagamento.setOrdemDeServico(os);
        pagamento.setFormaPagamento(request.formaPagamento());
        pagamento.setValor(request.valor());

        PagamentoResponse response = PagamentoResponse.from(pagamentoRepository.save(pagamento));
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/pagamentos/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/ordens/{osId}/pagamento")
    @Operation(
        summary = "Buscar pagamento da OS",
        description = "Retorna o pagamento associado à ordem de serviço informada."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Nenhum pagamento encontrado para a OS")
    })
    @Transactional(readOnly = true)
    public ResponseEntity<PagamentoResponse> buscarPorOS(
            @Parameter(description = "ID da ordem de serviço") @PathVariable UUID osId) {
        return ResponseEntity.ok(
                PagamentoResponse.from(
                        pagamentoRepository.findByOrdemDeServicoId(osId)
                                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado para a OS: " + osId))
                )
        );
    }

    @PatchMapping("/pagamentos/{id}/confirmar")
    @Operation(
        summary = "Confirmar pagamento",
        description = "Muda o status do pagamento de PENDENTE para PAGO e avança a OS para ENTREGUE."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagamento confirmado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado"),
        @ApiResponse(responseCode = "422", description = "Apenas pagamentos PENDENTES podem ser confirmados")
    })
    @Transactional
    public ResponseEntity<PagamentoResponse> confirmar(
            @Parameter(description = "ID do pagamento") @PathVariable UUID id) {
        var pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado: " + id));
        try {
            pagamento.confirmar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        var salvo = pagamentoRepository.save(pagamento);

        var os = pagamento.getOrdemDeServico();
        String emailCliente = os.getCliente().getEmail() != null
                ? os.getCliente().getEmail() : "sem-email@siase.com";

        emailPort.enviarConfirmacaoPagamento(
                emailCliente,
                os.getCliente().getNome(),
                os.getNumero(),
                pagamento.getValor().toPlainString(),
                pagamento.getFormaPagamento().getDescricao()
        );

        os.avancarStatus();
        ordemServicoRepository.save(os);

        return ResponseEntity.ok(PagamentoResponse.from(salvo));
    }

    @PatchMapping("/pagamentos/{id}/cancelar")
    @Operation(
        summary = "Cancelar pagamento",
        description = "Cancela um pagamento PENDENTE, liberando a OS para receber um novo pagamento."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagamento cancelado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado"),
        @ApiResponse(responseCode = "422", description = "Pagamentos já confirmados não podem ser cancelados")
    })
    @Transactional
    public ResponseEntity<PagamentoResponse> cancelar(
            @Parameter(description = "ID do pagamento") @PathVariable UUID id) {
        var pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado: " + id));
        try {
            pagamento.cancelar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        return ResponseEntity.ok(PagamentoResponse.from(pagamentoRepository.save(pagamento)));
    }
}
