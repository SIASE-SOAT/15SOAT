package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
import br.com.fiap.siase.infrastructure.persistence.entity.VeiculoEntity;
import br.com.fiap.siase.infrastructure.persistence.mapper.VeiculoMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class VeiculoRepositoryAdapter implements VeiculoRepositoryPort {

    private final VeiculoJpaRepository jpaRepository;
    private final VeiculoMapper mapper;

    public VeiculoRepositoryAdapter(VeiculoJpaRepository jpaRepository, VeiculoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Veiculo save(Veiculo veiculo) {
        VeiculoEntity entity = mapper.toEntity(veiculo);
        VeiculoEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Veiculo> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Veiculo> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Veiculo> findByPlaca(String placa) {
        return jpaRepository.findByPlaca(placa).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Veiculo> findByClienteId(UUID clienteId) {
        return jpaRepository.findByClienteId(clienteId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
