package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.AvancarStatusUCPort;
import br.com.fiap.siase.application.port.ObservabilityPort;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;

import java.util.UUID;
import java.time.Duration;
import java.time.LocalDateTime;

public class AvancarStatusUC implements AvancarStatusUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final ObservabilityPort observability;

    public AvancarStatusUC(OrdemServicoRepositoryPort ordemServicoRepository, ObservabilityPort observability) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.observability = observability;
    }

    @Override
    public OrdemDeServicoResponse executar(UUID osId) {
        OrdemDeServico os = ordemServicoRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + osId));
        var statusAnterior = os.getStatus();
        LocalDateTime statusDesde = os.getAtualizadoEm();
        try {
            os.avancarStatus();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        if (statusDesde != null && statusAnterior != null && statusAnterior.medeTempoDeNegocio()) {
            observability.tempoStatus(statusAnterior.name(),
                    Duration.between(statusDesde, LocalDateTime.now()).toNanos());
        }
        return OrdemDeServicoResponse.from(ordemServicoRepository.save(os));
    }

}
