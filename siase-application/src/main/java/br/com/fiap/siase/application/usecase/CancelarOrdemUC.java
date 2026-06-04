package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.CancelarOrdemUCPort;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;

import java.util.UUID;

public class CancelarOrdemUC implements CancelarOrdemUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;

    public CancelarOrdemUC(OrdemServicoRepositoryPort ordemServicoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
    }

    @Override
    public OrdemDeServicoResponse executar(UUID osId) {
        OrdemDeServico os = ordemServicoRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + osId));
        try {
            os.cancelar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        return OrdemDeServicoResponse.from(ordemServicoRepository.save(os));
    }
}
