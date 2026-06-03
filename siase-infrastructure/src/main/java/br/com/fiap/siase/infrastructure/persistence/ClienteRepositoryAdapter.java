package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.infrastructure.persistence.entity.ClienteEntity;
import br.com.fiap.siase.infrastructure.persistence.mapper.ClienteMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class ClienteRepositoryAdapter implements ClienteRepositoryPort {

    private final ClienteJpaRepository jpaRepository;
    private final ClienteMapper mapper;

    public ClienteRepositoryAdapter(ClienteJpaRepository jpaRepository, ClienteMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Cliente save(Cliente cliente) {
        ClienteEntity entity = mapper.toEntity(cliente);
        ClienteEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> findByDocumento(String documento) {
        return jpaRepository.findByDocumento(documento).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
