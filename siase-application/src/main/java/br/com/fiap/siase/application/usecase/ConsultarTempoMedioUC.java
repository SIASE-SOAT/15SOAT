package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.usecase.port.ConsultarTempoMedioUCPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;

public class ConsultarTempoMedioUC implements ConsultarTempoMedioUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;

    public ConsultarTempoMedioUC(OrdemServicoRepositoryPort ordemServicoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
    }

    @Override
    public Double executar() {
        return ordemServicoRepository.calcularTempoMedioExecucaoMinutos().orElse(0.0);
    }
}
