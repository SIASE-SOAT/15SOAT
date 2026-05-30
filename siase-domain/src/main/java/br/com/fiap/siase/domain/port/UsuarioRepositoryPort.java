package br.com.fiap.siase.domain.port;

import br.com.fiap.siase.domain.model.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositoryPort {

    Usuario save(Usuario usuario);

    Optional<Usuario> findById(UUID id);

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);
}
