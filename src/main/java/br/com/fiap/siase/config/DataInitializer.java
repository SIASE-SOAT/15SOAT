package br.com.fiap.siase.config;

import br.com.fiap.siase.model.Usuario;
import br.com.fiap.siase.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (!usuarioRepository.existsByUsername("mecanico")) {
            var mecanico = new Usuario();
            mecanico.setUsername("mecanico");
            mecanico.setPassword(passwordEncoder.encode("mecanico123"));
            usuarioRepository.save(mecanico);
            log.info("[DataInitializer] Usuário 'mecanico' criado com senha 'mecanico123'");
        }
    }
}
