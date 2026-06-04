package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.infrastructure.persistence.entity.OrdemDeServicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdemServicoJpaRepository extends JpaRepository<OrdemDeServicoEntity, UUID> {

    Optional<OrdemDeServicoEntity> findByNumero(String numero);

    List<OrdemDeServicoEntity> findByClienteId(UUID clienteId);

    List<OrdemDeServicoEntity> findByVeiculoId(UUID veiculoId);

    List<OrdemDeServicoEntity> findByStatus(StatusOS status);

    boolean existsByVeiculoIdAndStatusNotIn(UUID veiculoId, List<StatusOS> statuses);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (data_fim_execucao - data_inicio_execucao)) / 60.0) " +
                   "FROM itens_servico WHERE data_inicio_execucao IS NOT NULL AND data_fim_execucao IS NOT NULL",
           nativeQuery = true)
    Double calcularTempoMedioExecucaoMinutos();

    @Query(value = "SELECT * FROM ordens_de_servico " +
                   "WHERE status NOT IN ('FINALIZADA', 'ENTREGUE') " +
                   "ORDER BY " +
                   "CASE status " +
                   "  WHEN 'EM_EXECUCAO' THEN 1 " +
                   "  WHEN 'AGUARDANDO_APROVACAO' THEN 2 " +
                   "  WHEN 'EM_DIAGNOSTICO' THEN 3 " +
                   "  WHEN 'RECEBIDA' THEN 4 " +
                   "END, " +
                   "data_abertura ASC",
           nativeQuery = true)
    List<OrdemDeServicoEntity> findAllAtivasOrdered();
}
