package br.com.fiap.siase.domain.port;

import br.com.fiap.siase.domain.model.Cliente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepositoryPort {

    Cliente save(Cliente cliente);

    Optional<Cliente> findById(UUID id);

    List<Cliente> findAll();

    Optional<Cliente> findByDocumento(String documento);

    void deleteById(UUID id);
}
