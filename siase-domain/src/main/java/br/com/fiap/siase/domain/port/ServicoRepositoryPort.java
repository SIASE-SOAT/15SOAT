package br.com.fiap.siase.domain.port;

import br.com.fiap.siase.domain.model.Servico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRepositoryPort {

    Servico save(Servico servico);

    Optional<Servico> findById(UUID id);

    List<Servico> findAll();

    List<Servico> findByAtivoTrue();
}
