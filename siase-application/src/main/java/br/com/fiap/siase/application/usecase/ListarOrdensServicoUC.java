package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;

import java.util.List;

public class ListarOrdensServicoUC {

    private final OrdemServicoRepositoryPort ordemServicoRepository;

    public ListarOrdensServicoUC(OrdemServicoRepositoryPort ordemServicoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
    }

    public List<OrdemDeServicoResponse> executar() {
        return ordemServicoRepository.findAllAtivasOrdered().stream()
                .map(OrdemDeServicoResponse::from)
                .toList();
    }
}
