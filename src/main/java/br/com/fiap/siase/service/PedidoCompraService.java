package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.PedidoCompraRequest;
import br.com.fiap.siase.dto.response.PedidoCompraResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.PedidoCompra;
import br.com.fiap.siase.model.enums.StatusPedidoCompra;
import br.com.fiap.siase.repository.PecaRepository;
import br.com.fiap.siase.repository.PedidoCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PedidoCompraService {

    private final PedidoCompraRepository repository;
    private final PecaRepository pecaRepository;

    @Transactional
    public PedidoCompraResponse criar(PedidoCompraRequest request) {
        var peca = pecaRepository.findById(request.pecaId())
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + request.pecaId()));

        var pedido = new PedidoCompra();
        pedido.setPeca(peca);
        pedido.setQuantidadeSolicitada(request.quantidadeSolicitada());
        pedido.setObservacoes(request.observacoes());

        return PedidoCompraResponse.from(repository.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoCompraResponse> listar() {
        return repository.findAll().stream().map(PedidoCompraResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoCompraResponse> listarPorStatus(StatusPedidoCompra status) {
        return repository.findByStatus(status).stream().map(PedidoCompraResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PedidoCompraResponse buscarPorId(UUID id) {
        return PedidoCompraResponse.from(findOrThrow(id));
    }

    @Transactional
    public PedidoCompraResponse aprovar(UUID id) {
        var pedido = findOrThrow(id);
        try {
            pedido.aprovar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        return PedidoCompraResponse.from(repository.save(pedido));
    }

    @Transactional
    public PedidoCompraResponse receber(UUID id, int quantidadeRecebida) {
        var pedido = findOrThrow(id);

        if (quantidadeRecebida <= 0) {
            throw new BusinessException("Quantidade recebida deve ser maior que zero.");
        }

        try {
            pedido.receber(quantidadeRecebida);
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        pecaRepository.save(pedido.getPeca());
        return PedidoCompraResponse.from(repository.save(pedido));
    }

    @Transactional
    public PedidoCompraResponse cancelar(UUID id) {
        var pedido = findOrThrow(id);
        try {
            pedido.cancelar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        return PedidoCompraResponse.from(repository.save(pedido));
    }

    private PedidoCompra findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de compra não encontrado: " + id));
    }
}
