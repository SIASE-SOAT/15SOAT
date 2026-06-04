package br.com.fiap.siase.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("JwtService: geracao e validacao de tokens JWT")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = User.builder()
                .username("mecanico")
                .password("senha-hash")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("Deve gerar token JWT valido")
    void deveGerarTokenValido() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Deve extrair username do token")
    void deveExtrairUsernameDoToken() {
        String token = jwtService.generateToken(userDetails);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("mecanico");
    }

    @Test
    @DisplayName("Deve validar token para o usuario correto")
    void deveValidarTokenParaUsuarioCorreto() {
        String token = jwtService.generateToken(userDetails);

        boolean valido = jwtService.isTokenValid(token, userDetails);

        assertThat(valido).isTrue();
    }

    @Test
    @DisplayName("Nao deve validar token para usuario diferente")
    void naoDeveValidarTokenParaUsuarioDiferente() {
        String token = jwtService.generateToken(userDetails);

        UserDetails outroUsuario = User.builder()
                .username("outro-usuario")
                .password("outra-senha")
                .authorities(Collections.emptyList())
                .build();

        boolean valido = jwtService.isTokenValid(token, outroUsuario);

        assertThat(valido).isFalse();
    }
}
