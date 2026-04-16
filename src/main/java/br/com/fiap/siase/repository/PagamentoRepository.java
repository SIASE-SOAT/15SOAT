package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    Optional<Pagamento> findByOrdemDeServicoId(UUID ordemDeServicoId);

    boolean existsByOrdemDeServicoId(UUID ordemDeServicoId);
}
