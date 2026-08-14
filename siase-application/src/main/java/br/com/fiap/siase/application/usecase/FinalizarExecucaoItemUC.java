package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.FinalizarExecucaoItemUCPort;
import br.com.fiap.siase.application.port.ObservabilityPort;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;

import java.time.Duration;
import java.util.UUID;

public class FinalizarExecucaoItemUC implements FinalizarExecucaoItemUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final ObservabilityPort observability;

    public FinalizarExecucaoItemUC(OrdemServicoRepositoryPort ordemServicoRepository, ObservabilityPort observability) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.observability = observability;
    }

    @Override
    public OrdemDeServicoResponse executar(UUID osId, UUID itemId) {
        var os = ordemServicoRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + osId));

        if (os.getStatus() != StatusOS.EM_EXECUCAO) {
            throw new BusinessException("Só é possível finalizar a execução de serviços quando a OS está em execução.");
        }

        var item = os.getItensServico().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item de serviço não encontrado: " + itemId));

        item.finalizarExecucao();
        if (item.getDataInicioExecucao() != null && item.getDataFimExecucao() != null) {
            observability.tempoExecucaoItem(Duration.between(
                    item.getDataInicioExecucao(), item.getDataFimExecucao()).toNanos());
        }
        return OrdemDeServicoResponse.from(ordemServicoRepository.save(os));
    }
}
