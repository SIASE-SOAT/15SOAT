package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.enums.StatusPedidoCompra;
import br.com.fiap.siase.domain.model.PedidoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PedidoCompraJpaRepository extends JpaRepository<PedidoCompra, UUID> {

    List<PedidoCompra> findByStatus(StatusPedidoCompra status);
}
