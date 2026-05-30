package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VeiculoRepositoryAdapter implements VeiculoRepositoryPort {

    private final VeiculoJpaRepository jpaRepository;

    public VeiculoRepositoryAdapter(VeiculoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Veiculo save(Veiculo veiculo) {
        return jpaRepository.save(veiculo);
    }

    @Override
    public Optional<Veiculo> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Veiculo> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Optional<Veiculo> findByPlaca(String placa) {
        return jpaRepository.findByPlaca(placa);
    }

    @Override
    public List<Veiculo> findByClienteId(UUID clienteId) {
        return jpaRepository.findByClienteId(clienteId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
