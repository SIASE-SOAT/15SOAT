package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.IniciarExecucaoItemUCPort;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;

import java.util.UUID;

public class IniciarExecucaoItemUC implements IniciarExecucaoItemUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;

    public IniciarExecucaoItemUC(OrdemServicoRepositoryPort ordemServicoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
    }

    @Override
    public OrdemDeServicoResponse executar(UUID osId, UUID itemId) {
        var os = ordemServicoRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + osId));

        if (os.getStatus() != StatusOS.EM_EXECUCAO) {
            throw new BusinessException("Só é possível iniciar a execução de serviços quando a OS está em execução.");
        }

        var item = os.getItensServico().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item de serviço não encontrado: " + itemId));

        item.iniciarExecucao();
        return OrdemDeServicoResponse.from(ordemServicoRepository.save(os));
    }
}
