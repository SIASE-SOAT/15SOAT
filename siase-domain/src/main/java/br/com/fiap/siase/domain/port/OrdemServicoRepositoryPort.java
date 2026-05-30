package br.com.fiap.siase.domain.port;

import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.model.OrdemDeServico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdemServicoRepositoryPort {

    OrdemDeServico save(OrdemDeServico ordemDeServico);

    Optional<OrdemDeServico> findById(UUID id);

    List<OrdemDeServico> findAll();

    List<OrdemDeServico> findByStatus(StatusOS status);

    Optional<OrdemDeServico> findByNumero(String numero);

    List<OrdemDeServico> findByClienteId(UUID clienteId);

    List<OrdemDeServico> findByVeiculoId(UUID veiculoId);

    boolean existsByVeiculoIdAndStatusNotIn(UUID veiculoId, List<StatusOS> statuses);

    List<OrdemDeServico> findAllAtivasOrdered();
}
