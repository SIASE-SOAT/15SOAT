package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Servico;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ServicoRepositoryAdapter implements ServicoRepositoryPort {

    private final ServicoJpaRepository jpaRepository;

    public ServicoRepositoryAdapter(ServicoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Servico save(Servico servico) {
        return jpaRepository.save(servico);
    }

    @Override
    public Optional<Servico> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Servico> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Servico> findByAtivoTrue() {
        return jpaRepository.findByAtivoTrue();
    }
}
