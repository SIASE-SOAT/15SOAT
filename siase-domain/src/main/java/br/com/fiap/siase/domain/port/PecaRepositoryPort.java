package br.com.fiap.siase.domain.port;

import br.com.fiap.siase.domain.model.Peca;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PecaRepositoryPort {

    Peca save(Peca peca);

    Optional<Peca> findById(UUID id);

    Optional<Peca> findByIdParaAtualizacao(UUID id);

    List<Peca> findAll();

    List<Peca> findByAtivoTrue();
}
