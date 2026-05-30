package br.com.fiap.siase.domain.port;

import br.com.fiap.siase.domain.model.Veiculo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeiculoRepositoryPort {

    Veiculo save(Veiculo veiculo);

    Optional<Veiculo> findById(UUID id);

    List<Veiculo> findAll();

    Optional<Veiculo> findByPlaca(String placa);

    List<Veiculo> findByClienteId(UUID clienteId);

    void deleteById(UUID id);
}
