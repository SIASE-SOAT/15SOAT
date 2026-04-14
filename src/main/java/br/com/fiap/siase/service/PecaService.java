package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.MovimentacaoEstoqueRequest;
import br.com.fiap.siase.dto.request.PecaRequest;
import br.com.fiap.siase.dto.response.PecaResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Peca;
import br.com.fiap.siase.repository.PecaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PecaService {

    private final PecaRepository repository;

    @Transactional(readOnly = true)
    public List<PecaResponse> listarAtivas() {
        return repository.findByAtivoTrue().stream()
                .map(PecaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PecaResponse> listarTodas() {
        return repository.findAll().stream()
                .map(PecaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PecaResponse buscarPorId(UUID id) {
        return PecaResponse.from(findOrThrow(id));
    }

    @Transactional
    public PecaResponse criar(PecaRequest request) {
        var peca = new Peca();
        peca.setCodigo(request.codigo().toUpperCase().trim());
        peca.setNome(request.nome());
        peca.setDescricao(request.descricao());
        peca.setPreco(request.preco());
        peca.setQuantidadeEstoque(request.quantidadeEstoque() != null ? request.quantidadeEstoque() : 0);
        peca.setEstoqueMinimo(request.estoqueMinimo() != null ? request.estoqueMinimo() : 0);
        peca.setUnidadeMedida(request.unidadeMedida() != null ? request.unidadeMedida().toUpperCase().trim() : "UN");
        return PecaResponse.from(repository.save(peca));
    }

    @Transactional
    public PecaResponse atualizar(UUID id, PecaRequest request) {
        var peca = findOrThrow(id);
        peca.setCodigo(request.codigo().toUpperCase().trim());
        peca.setNome(request.nome());
        peca.setDescricao(request.descricao());
        peca.setPreco(request.preco());
        peca.setEstoqueMinimo(request.estoqueMinimo() != null ? request.estoqueMinimo() : peca.getEstoqueMinimo());
        peca.setUnidadeMedida(request.unidadeMedida() != null ? request.unidadeMedida().toUpperCase().trim() : peca.getUnidadeMedida());
        return PecaResponse.from(repository.save(peca));
    }

    @Transactional
    public PecaResponse movimentarEstoque(UUID id, MovimentacaoEstoqueRequest request) {
        var peca = findOrThrow(id);

        if (request.operacao() == MovimentacaoEstoqueRequest.Operacao.ENTRADA) {
            peca.devolverEstoque(request.quantidade());
        } else {
            if (!peca.temEstoque(request.quantidade())) {
                throw new BusinessException(
                    "Estoque insuficiente. Disponível: " + peca.getQuantidadeEstoque()
                    + ", solicitado: " + request.quantidade()
                );
            }
            peca.reservarEstoque(request.quantidade());
        }

        return PecaResponse.from(repository.save(peca));
    }

    @Transactional
    public void desativar(UUID id) {
        var peca = findOrThrow(id);
        peca.setAtivo(false);
        repository.save(peca);
    }

    public Peca findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + id));
    }
}
