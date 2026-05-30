package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteJpaRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByDocumento(String documento);
}
