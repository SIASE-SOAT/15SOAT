package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.input.ItemPecaRequest;
import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.AdicionarPecaUCPort;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.ItemPeca;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.model.Peca;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;

import java.util.UUID;

public class AdicionarPecaUC implements AdicionarPecaUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final PecaRepositoryPort pecaRepository;

    public AdicionarPecaUC(OrdemServicoRepositoryPort ordemServicoRepository, PecaRepositoryPort pecaRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.pecaRepository = pecaRepository;
    }

    @Override
    public OrdemDeServicoResponse executar(UUID osId, ItemPecaRequest request) {
        OrdemDeServico os = ordemServicoRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + osId));

        Peca peca = pecaRepository.findByIdParaAtualizacao(request.pecaId())
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + request.pecaId()));

        if (!Boolean.TRUE.equals(peca.getAtivo())) {
            throw new BusinessException("Peça desativada não pode ser adicionada: " + request.pecaId());
        }

        boolean jaExiste = os.getItensPeca().stream()
                .anyMatch(item -> item.getPeca().getId().equals(request.pecaId()));
        if (jaExiste) {
            throw new BusinessException("Esta peça já foi adicionada a esta ordem de serviço.");
        }

        try {
            peca.reservarEstoque(request.quantidade());
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        pecaRepository.save(peca);

        ItemPeca item = new ItemPeca();
        item.setOrdemDeServico(os);
        item.setPeca(peca);
        item.setQuantidade(request.quantidade());
        item.setPrecoUnitario(peca.getPreco());
        os.getItensPeca().add(item);
        os.recalcularTotais();

        return OrdemDeServicoResponse.from(ordemServicoRepository.save(os));
    }
}
