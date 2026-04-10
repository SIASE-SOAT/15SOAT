package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.ServicoInsumoRequest;
import br.com.fiap.siase.dto.request.ServicoRequest;
import br.com.fiap.siase.dto.response.ServicoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Servico;
import br.com.fiap.siase.model.ServicoInsumo;
import br.com.fiap.siase.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository repository;
    private final PecaService pecaService;

    public List<ServicoResponse> listarAtivos() {
        return repository.findByAtivoTrue().stream()
                .map(ServicoResponse::from)
                .toList();
    }

    public List<ServicoResponse> listarTodos() {
        return repository.findAll().stream()
                .map(ServicoResponse::from)
                .toList();
    }

    public ServicoResponse buscarPorId(UUID id) {
        return ServicoResponse.from(findOrThrow(id));
    }

    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        var servico = new Servico();
        servico.setNome(request.nome());
        servico.setDescricao(request.descricao());
        servico.setPreco(request.preco());
        return ServicoResponse.from(repository.save(servico));
    }

    @Transactional
    public ServicoResponse atualizar(UUID id, ServicoRequest request) {
        var servico = findOrThrow(id);
        servico.setNome(request.nome());
        servico.setDescricao(request.descricao());
        servico.setPreco(request.preco());
        return ServicoResponse.from(repository.save(servico));
    }

    @Transactional
    public ServicoResponse adicionarInsumo(UUID servicoId, ServicoInsumoRequest request) {
        var servico = findOrThrow(servicoId);
        var peca    = pecaService.findOrThrow(request.pecaId());

        boolean jaExiste = servico.getInsumos().stream()
                .anyMatch(i -> i.getPeca().getId().equals(request.pecaId()));
        if (jaExiste) {
            throw new BusinessException("Peça '" + peca.getNome() + "' já está vinculada a este serviço. Use PUT para atualizar a quantidade.");
        }

        servico.getInsumos().add(new ServicoInsumo(servico, peca, request.quantidade()));
        return ServicoResponse.from(repository.save(servico));
    }

    @Transactional
    public ServicoResponse atualizarInsumo(UUID servicoId, UUID pecaId, ServicoInsumoRequest request) {
        var servico = findOrThrow(servicoId);

        var insumo = servico.getInsumos().stream()
                .filter(i -> i.getPeca().getId().equals(pecaId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Peça não vinculada a este serviço: " + pecaId));

        insumo.setQuantidade(request.quantidade());
        return ServicoResponse.from(repository.save(servico));
    }

    @Transactional
    public ServicoResponse removerInsumo(UUID servicoId, UUID pecaId) {
        var servico = findOrThrow(servicoId);

        boolean removido = servico.getInsumos()
                .removeIf(i -> i.getPeca().getId().equals(pecaId));

        if (!removido) {
            throw new ResourceNotFoundException("Peça não vinculada a este serviço: " + pecaId);
        }

        return ServicoResponse.from(repository.save(servico));
    }

    @Transactional
    public void desativar(UUID id) {
        var servico = findOrThrow(id);
        servico.setAtivo(false);
        repository.save(servico);
    }

    private Servico findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + id));
    }
}
