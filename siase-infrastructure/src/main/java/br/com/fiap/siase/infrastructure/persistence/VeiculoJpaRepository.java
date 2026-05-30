package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VeiculoJpaRepository extends JpaRepository<Veiculo, UUID> {

    Optional<Veiculo> findByPlaca(String placa);

    List<Veiculo> findByClienteId(UUID clienteId);
}
