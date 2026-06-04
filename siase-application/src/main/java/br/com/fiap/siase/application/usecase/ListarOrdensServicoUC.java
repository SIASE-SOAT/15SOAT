package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.ListarOrdensServicoUCPort;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;

import java.util.List;

public class ListarOrdensServicoUC implements ListarOrdensServicoUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;

    public ListarOrdensServicoUC(OrdemServicoRepositoryPort ordemServicoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
    }

    @Override
    public List<OrdemDeServicoResponse> executar(StatusOS status) {
        var ordens = status != null
                ? ordemServicoRepository.findByStatus(status)
                : ordemServicoRepository.findAllAtivasOrdered();
        return ordens.stream()
                .map(OrdemDeServicoResponse::from)
                .toList();
    }
}
