package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.infrastructure.persistence.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, UUID> {

    Optional<ClienteEntity> findByDocumento(String documento);
}
