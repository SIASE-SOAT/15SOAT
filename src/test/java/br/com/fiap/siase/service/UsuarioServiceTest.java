package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.RegistroRequest;
import br.com.fiap.siase.dto.response.UsuarioResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.model.Usuario;
import br.com.fiap.siase.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("UsuarioService - Registro de usuários")
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository repository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService service;

    private Usuario usuarioSalvo;

    @BeforeEach
    void setUp() {
        usuarioSalvo = new Usuario();
        ReflectionTestUtils.setField(usuarioSalvo, "id", UUID.randomUUID());
        usuarioSalvo.setUsername("atendente1");
        usuarioSalvo.setPassword("hash_bcrypt");
        ReflectionTestUtils.setField(usuarioSalvo, "criadoEm", LocalDateTime.now());
    }

    @Nested
    @DisplayName("Registro bem-sucedido")
    class RegistroBemSucedido {

        @Test
        @DisplayName("Deve retornar UsuarioResponse com id e username após salvar")
        void deveRetornarResponseAoRegistrar() {
            when(repository.existsByUsername("atendente1")).thenReturn(false);
            when(passwordEncoder.encode("Atend@2024")).thenReturn("hash_bcrypt");
            when(repository.save(any())).thenReturn(usuarioSalvo);

            UsuarioResponse response = service.registrar(new RegistroRequest("atendente1", "Atend@2024"));

            assertThat(response.username()).isEqualTo("atendente1");
            assertThat(response.id()).isNotNull();
            assertThat(response.criadoEm()).isNotNull();
        }

        @Test
        @DisplayName("Deve salvar senha encriptada, nunca a senha em texto puro")
        void deveSalvarSenhaEncriptada() {
            when(repository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode("Atend@2024")).thenReturn("$2a$10$hash");
            when(repository.save(any())).thenReturn(usuarioSalvo);

            service.registrar(new RegistroRequest("atendente1", "Atend@2024"));

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(repository).save(captor.capture());

            assertThat(captor.getValue().getPassword())
                    .isEqualTo("$2a$10$hash")
                    .isNotEqualTo("Atend@2024");
        }

        @Test
        @DisplayName("Deve salvar o username exatamente como informado")
        void deveSalvarUsernameCorreto() {
            when(repository.existsByUsername("atendente1")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(repository.save(any())).thenReturn(usuarioSalvo);

            service.registrar(new RegistroRequest("atendente1", "Atend@2024"));

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getUsername()).isEqualTo("atendente1");
        }

        @Test
        @DisplayName("Deve verificar existência do username antes de salvar")
        void deveVerificarExistenciaDoUsername() {
            when(repository.existsByUsername("atendente1")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(repository.save(any())).thenReturn(usuarioSalvo);

            service.registrar(new RegistroRequest("atendente1", "Atend@2024"));

            verify(repository).existsByUsername("atendente1");
        }
    }

    @Nested
    @DisplayName("Registro com falha")
    class RegistroComFalha {

        @Test
        @DisplayName("Deve lançar BusinessException quando username já está em uso")
        void deveLancarExcecaoQuandoUsernameJaExiste() {
            when(repository.existsByUsername("atendente1")).thenReturn(true);

            assertThatThrownBy(() -> service.registrar(new RegistroRequest("atendente1", "Atend@2024")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("atendente1");
        }

        @Test
        @DisplayName("Não deve chamar passwordEncoder quando username já existe")
        void naoDeveChamarEncoderQuandoUsernameJaExiste() {
            when(repository.existsByUsername("atendente1")).thenReturn(true);

            assertThatThrownBy(() -> service.registrar(new RegistroRequest("atendente1", "Atend@2024")))
                    .isInstanceOf(BusinessException.class);

            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("Não deve salvar no banco quando username já existe")
        void naoDeveSalvarQuandoUsernameJaExiste() {
            when(repository.existsByUsername("atendente1")).thenReturn(true);

            assertThatThrownBy(() -> service.registrar(new RegistroRequest("atendente1", "Atend@2024")))
                    .isInstanceOf(BusinessException.class);

            verify(repository, never()).save(any());
        }
    }
}
