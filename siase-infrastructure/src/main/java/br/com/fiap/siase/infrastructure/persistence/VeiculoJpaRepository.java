package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.infrastructure.persistence.entity.VeiculoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VeiculoJpaRepository extends JpaRepository<VeiculoEntity, UUID> {

    Optional<VeiculoEntity> findByPlaca(String placa);

    List<VeiculoEntity> findByClienteId(UUID clienteId);
}
