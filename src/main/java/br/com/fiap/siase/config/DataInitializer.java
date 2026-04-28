package br.com.fiap.siase.config;

import br.com.fiap.siase.model.Usuario;
import br.com.fiap.siase.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.mecanico.password:mecanico123}")
    private String mecanicoPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (!usuarioRepository.existsByUsername("mecanico")) {
            var mecanico = new Usuario();
            mecanico.setUsername("mecanico");
            mecanico.setPassword(passwordEncoder.encode(mecanicoPassword));
            usuarioRepository.save(mecanico);
            log.info("[DataInitializer] Usuário 'mecanico' criado.");
        }
    }
}
