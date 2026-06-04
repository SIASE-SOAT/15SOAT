package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Pagamento;
import br.com.fiap.siase.domain.port.PagamentoRepositoryPort;
import br.com.fiap.siase.infrastructure.persistence.mapper.PagamentoMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class PagamentoRepositoryAdapter implements PagamentoRepositoryPort {

    private final PagamentoJpaRepository jpaRepository;
    private final PagamentoMapper mapper;

    public PagamentoRepositoryAdapter(PagamentoJpaRepository jpaRepository, PagamentoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Pagamento save(Pagamento pagamento) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(pagamento)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pagamento> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pagamento> findByOrdemDeServicoId(UUID osId) {
        return jpaRepository.findByOrdemDeServicoId(osId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOrdemDeServicoId(UUID osId) {
        return jpaRepository.existsByOrdemDeServicoId(osId);
    }
}
