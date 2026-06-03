package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Peca;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import br.com.fiap.siase.infrastructure.persistence.entity.PecaEntity;
import br.com.fiap.siase.infrastructure.persistence.mapper.PecaMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class PecaRepositoryAdapter implements PecaRepositoryPort {

    private final PecaJpaRepository jpaRepository;
    private final PecaMapper mapper;

    public PecaRepositoryAdapter(PecaJpaRepository jpaRepository, PecaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Peca save(Peca peca) {
        PecaEntity entity = mapper.toEntity(peca);
        PecaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Peca> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Peca> findByIdParaAtualizacao(UUID id) {
        return jpaRepository.findByIdParaAtualizacao(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Peca> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Peca> findByAtivoTrue() {
        return jpaRepository.findByAtivoTrue().stream().map(mapper::toDomain).toList();
    }
}
