package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.OrdemDeServico;
import br.com.fiap.siase.model.enums.StatusOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdemDeServicoRepository extends JpaRepository<OrdemDeServico, UUID> {

    Optional<OrdemDeServico> findByNumero(String numero);

    List<OrdemDeServico> findByClienteId(UUID clienteId);

    List<OrdemDeServico> findByVeiculoId(UUID veiculoId);

    List<OrdemDeServico> findByStatus(StatusOS status);

    boolean existsByVeiculoIdAndStatusNotIn(UUID veiculoId, List<StatusOS> statuses);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (data_fim_execucao - data_inicio_execucao)) / 60.0) " +
                   "FROM itens_servico WHERE data_inicio_execucao IS NOT NULL AND data_fim_execucao IS NOT NULL",
           nativeQuery = true)
    Double calcularTempoMedioExecucaoMinutos();
}
