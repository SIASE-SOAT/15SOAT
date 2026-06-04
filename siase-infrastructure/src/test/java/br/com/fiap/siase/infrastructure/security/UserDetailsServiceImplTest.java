package br.com.fiap.siase.infrastructure.security;

import br.com.fiap.siase.domain.model.Usuario;
import br.com.fiap.siase.domain.port.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl: carregamento de usuario por username")
class UserDetailsServiceImplTest {

    @Mock private UsuarioRepositoryPort usuarioRepositoryPort;

    private UserDetailsServiceImpl service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new UserDetailsServiceImpl(usuarioRepositoryPort);

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setUsername("mecanico");
        usuario.setPassword("$2a$10$hashedpassword");
        usuario.setAtivo(true);
    }

    @Test
    @DisplayName("Deve carregar usuario existente pelo username")
    void deveCarregarUsuarioExistente() {
        when(usuarioRepositoryPort.findByUsername("mecanico")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = service.loadUserByUsername("mecanico");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("mecanico");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedpassword");
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Deve lancar UsernameNotFoundException quando usuario nao encontrado")
    void deveLancarErroQuandoUsuarioNaoEncontrado() {
        when(usuarioRepositoryPort.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("inexistente"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("inexistente");
    }

    @Test
    @DisplayName("Deve retornar usuario inativo como nao habilitado")
    void deveRetornarUsuarioInativoComoNaoHabilitado() {
        usuario.setAtivo(false);
        when(usuarioRepositoryPort.findByUsername("mecanico")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = service.loadUserByUsername("mecanico");

        assertThat(userDetails.isEnabled()).isFalse();
    }
}
