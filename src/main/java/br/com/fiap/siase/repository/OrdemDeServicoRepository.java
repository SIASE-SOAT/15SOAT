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

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (data_fechamento - data_abertura)) / 60.0) " +
                   "FROM ordens_de_servico WHERE data_fechamento IS NOT NULL",
           nativeQuery = true)
    Double calcularTempoMedioExecucaoMinutos();
}
