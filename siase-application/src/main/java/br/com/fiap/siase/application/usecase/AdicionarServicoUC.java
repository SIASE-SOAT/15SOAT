package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.input.ItemServicoRequest;
import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.AdicionarServicoUCPort;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.ItemServico;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.model.Servico;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;

import java.util.UUID;

public class AdicionarServicoUC implements AdicionarServicoUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final ServicoRepositoryPort servicoRepository;

    public AdicionarServicoUC(OrdemServicoRepositoryPort ordemServicoRepository, ServicoRepositoryPort servicoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.servicoRepository = servicoRepository;
    }

    @Override
    public OrdemDeServicoResponse executar(UUID osId, ItemServicoRequest request) {
        OrdemDeServico os = ordemServicoRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + osId));

        Servico servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + request.servicoId()));

        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new BusinessException("Serviço desativado não pode ser adicionado: " + request.servicoId());
        }

        boolean jaExiste = os.getItensServico().stream()
                .anyMatch(item -> item.getServico().getId().equals(request.servicoId()));
        if (jaExiste) {
            throw new BusinessException("Este serviço já foi adicionado a esta ordem de serviço.");
        }

        ItemServico item = new ItemServico();
        item.setOrdemDeServico(os);
        item.setServico(servico);
        item.setPrecoUnitario(servico.getPreco());
        item.setTempoEstimadoMinutos(servico.getTempoEstimadoMinutos());
        item.setObservacoes(request.observacoes());
        os.getItensServico().add(item);
        os.recalcularTotais();

        return OrdemDeServicoResponse.from(ordemServicoRepository.save(os));
    }
}
