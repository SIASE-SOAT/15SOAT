package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagamentoJpaRepository extends JpaRepository<Pagamento, UUID> {

    Optional<Pagamento> findByOrdemDeServicoId(UUID ordemDeServicoId);

    boolean existsByOrdemDeServicoId(UUID ordemDeServicoId);
}
