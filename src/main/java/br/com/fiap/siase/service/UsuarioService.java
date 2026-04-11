package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.RegistroRequest;
import br.com.fiap.siase.dto.response.UsuarioResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.model.Usuario;
import br.com.fiap.siase.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        if (repository.existsByUsername(request.username())) {
            throw new BusinessException("Username '" + request.username() + "' já está em uso.");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(passwordEncoder.encode(request.password()));

        return UsuarioResponse.from(repository.save(usuario));
    }
}
