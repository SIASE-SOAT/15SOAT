package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;

import java.util.UUID;

public class ConsultarStatusOSUC {

    private final OrdemServicoRepositoryPort ordemServicoRepository;

    public ConsultarStatusOSUC(OrdemServicoRepositoryPort ordemServicoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
    }

    public OrdemDeServicoResponse executar(UUID id) {
        var os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + id));
        return OrdemDeServicoResponse.from(os);
    }

    public OrdemDeServicoResponse executarPorNumero(String numero) {
        var os = ordemServicoRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + numero));
        return OrdemDeServicoResponse.from(os);
    }
}
