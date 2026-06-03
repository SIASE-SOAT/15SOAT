package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.enums.StatusPedidoCompra;
import br.com.fiap.siase.domain.model.PedidoCompra;
import br.com.fiap.siase.domain.port.PedidoCompraRepositoryPort;
import br.com.fiap.siase.infrastructure.persistence.mapper.PedidoCompraMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class PedidoCompraRepositoryAdapter implements PedidoCompraRepositoryPort {

    private final PedidoCompraJpaRepository jpaRepository;
    private final PedidoCompraMapper mapper;

    public PedidoCompraRepositoryAdapter(PedidoCompraJpaRepository jpaRepository, PedidoCompraMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PedidoCompra save(PedidoCompra pedidoCompra) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(pedidoCompra)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PedidoCompra> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoCompra> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoCompra> findByStatus(StatusPedidoCompra status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).toList();
    }
}
