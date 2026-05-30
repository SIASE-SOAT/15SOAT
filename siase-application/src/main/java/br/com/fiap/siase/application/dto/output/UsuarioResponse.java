package br.com.fiap.siase.application.dto.output;

import br.com.fiap.siase.domain.model.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponse(UUID id, String username, LocalDateTime criadoEm) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getCriadoEm());
    }
}
