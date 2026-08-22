package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.AtualizarStatusViaWebhookUCPort;
import br.com.fiap.siase.application.port.ObservabilityPort;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.port.EmailPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import java.time.Duration;
import java.time.LocalDateTime;

public class AtualizarStatusViaWebhookUC implements AtualizarStatusViaWebhookUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final EmailPort emailPort;
    private final String webhookToken;
    private final ObservabilityPort observability;

    public AtualizarStatusViaWebhookUC(OrdemServicoRepositoryPort ordemServicoRepository, EmailPort emailPort,
                                       String webhookToken, ObservabilityPort observability) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.emailPort = emailPort;
        this.webhookToken = webhookToken;
        this.observability = observability;
    }

    public OrdemDeServicoResponse executar(String numero, String novoStatus, String tokenExterno) {
        if (!webhookToken.equals(tokenExterno)) {
            observability.falhaIntegracao("webhook");
            throw new BusinessException("Token de serviço externo inválido.");
        }

        var os = ordemServicoRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + numero));

        StatusOS statusAlvo;
        try {
            statusAlvo = StatusOS.valueOf(novoStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status inválido: " + novoStatus);
        }

        var statusAnterior = os.getStatus();
        var statusDesde = os.getAtualizadoEm();
        if (statusAlvo == StatusOS.CANCELADA) {
            os.cancelar();
        } else if (os.podeAvancarPara(statusAlvo)) {
            os.avancarStatus();
        } else {
            throw new BusinessException(
                    "Transição de status inválida: " + os.getStatus() + " -> " + statusAlvo);
        }

        if (statusDesde != null && statusAnterior != null && statusAnterior.medeTempoDeNegocio()) {
            observability.tempoStatus(statusAnterior.name(),
                    Duration.between(statusDesde, LocalDateTime.now()).toNanos());
        }
        var salvo = ordemServicoRepository.save(os);
        notificarCliente(os);
        return OrdemDeServicoResponse.from(salvo);
    }

    private void notificarCliente(OrdemDeServico os) {
        String email = os.getCliente().getEmail() != null
                ? os.getCliente().getEmail() : "sem-email@siase.com";
        String nome = os.getCliente().getNome();
        String numero = os.getNumero();

        switch (os.getStatus()) {
            case APROVADO -> emailPort.enviarOrcamentoAprovado(email, nome, numero);
            case CANCELADA -> emailPort.enviarOrcamentoCancelado(email, nome, numero);
            default -> { /* sem notificação para outros status */ }
        }
    }

}
