package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {

    Optional<Veiculo> findByPlaca(String placa);

    boolean existsByPlaca(String placa);

    List<Veiculo> findByClienteId(UUID clienteId);
}
