package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Servico;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;
import br.com.fiap.siase.infrastructure.persistence.entity.ServicoEntity;
import br.com.fiap.siase.infrastructure.persistence.mapper.ServicoMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class ServicoRepositoryAdapter implements ServicoRepositoryPort {

    private final ServicoJpaRepository jpaRepository;
    private final ServicoMapper mapper;

    public ServicoRepositoryAdapter(ServicoJpaRepository jpaRepository, ServicoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Servico save(Servico servico) {
        ServicoEntity entity = mapper.toEntity(servico);
        ServicoEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Servico> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Servico> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Servico> findByAtivoTrue() {
        return jpaRepository.findByAtivoTrue().stream().map(mapper::toDomain).toList();
    }
}
