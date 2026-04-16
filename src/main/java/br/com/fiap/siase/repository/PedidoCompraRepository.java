package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.PedidoCompra;
import br.com.fiap.siase.model.enums.StatusPedidoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, UUID> {

    List<PedidoCompra> findByStatus(StatusPedidoCompra status);

    List<PedidoCompra> findByPecaId(UUID pecaId);
}
