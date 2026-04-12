package br.com.fiap.siase.dto.response;

import br.com.fiap.siase.model.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponse(UUID id, String username, LocalDateTime criadoEm) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getCriadoEm());
    }
}
