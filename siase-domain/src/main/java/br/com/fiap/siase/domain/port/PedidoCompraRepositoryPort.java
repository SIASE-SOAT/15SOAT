package br.com.fiap.siase.domain.port;

import br.com.fiap.siase.domain.enums.StatusPedidoCompra;
import br.com.fiap.siase.domain.model.PedidoCompra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoCompraRepositoryPort {
    PedidoCompra save(PedidoCompra pedidoCompra);
    Optional<PedidoCompra> findById(UUID id);
    List<PedidoCompra> findAll();
    List<PedidoCompra> findByStatus(StatusPedidoCompra status);
}
