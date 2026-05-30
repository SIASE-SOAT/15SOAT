package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Peca;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PecaRepositoryAdapter implements PecaRepositoryPort {

    private final PecaJpaRepository jpaRepository;

    public PecaRepositoryAdapter(PecaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Peca save(Peca peca) {
        return jpaRepository.save(peca);
    }

    @Override
    public Optional<Peca> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Peca> findByIdParaAtualizacao(UUID id) {
        return jpaRepository.findByIdParaAtualizacao(id);
    }

    @Override
    public List<Peca> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Peca> findByAtivoTrue() {
        return jpaRepository.findByAtivoTrue();
    }
}
