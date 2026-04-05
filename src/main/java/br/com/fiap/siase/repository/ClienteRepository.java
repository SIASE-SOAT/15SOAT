package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByDocumento(String documento);

    boolean existsByDocumento(String documento);
}
