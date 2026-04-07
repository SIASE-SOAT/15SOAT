package br.com.fiap.siase.security;

import br.com.fiap.siase.model.Usuario;
import br.com.fiap.siase.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UserDetailsServiceImpl - Carregamento de usuário")
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Deve retornar UserDetails quando usuário existe")
    void deveRetornarUserDetailsQuandoUsuarioExiste() {
        Usuario usuario = new Usuario();
        usuario.setUsername("atendente1");
        usuario.setPassword("hash");
        usuario.setAtivo(true);

        when(usuarioRepository.findByUsername("atendente1")).thenReturn(Optional.of(usuario));

        UserDetails result = userDetailsService.loadUserByUsername("atendente1");

        assertThat(result.getUsername()).isEqualTo("atendente1");
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando usuário não existe")
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("inexistente"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("inexistente");
    }

    @Test
    @DisplayName("Deve chamar o repositório com o username informado")
    void deveChamarRepositorioComUsernameCorreto() {
        when(usuarioRepository.findByUsername("atendente2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("atendente2"))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(usuarioRepository).findByUsername("atendente2");
    }
}
