package br.com.fiap.siase.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService - Geração e validação de tokens")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "dGVzdFNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3NlczEyMzQ1Njc4OTA=";
    private static final long EXPIRATION_MS = 3_600_000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
    }

    private UserDetails userDetails(String username) {
        return User.withUsername(username).password("irrelevant").authorities(Collections.emptyList()).build();
    }

    @Nested
    @DisplayName("Geração de token")
    class GeracaoDeToken {

        @Test
        @DisplayName("Deve gerar token não nulo para usuário válido")
        void deveGerarTokenNaoNulo() {
            String token = jwtService.generateToken(userDetails("atendente1"));
            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Deve gerar tokens diferentes para usuários diferentes")
        void deveGerarTokensDiferentesParaUsuariosDiferentes() {
            String token1 = jwtService.generateToken(userDetails("atendente1"));
            String token2 = jwtService.generateToken(userDetails("atendente2"));
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Token deve conter três partes separadas por ponto")
        void tokenDeveSerJwt() {
            String token = jwtService.generateToken(userDetails("atendente1"));
            assertThat(token.split("\\.")).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Extração de username")
    class ExtracaoDeUsername {

        @Test
        @DisplayName("Deve extrair username correto do token")
        void deveExtrairUsernameCorreto() {
            UserDetails user = userDetails("atendente1");
            String token = jwtService.generateToken(user);
            assertThat(jwtService.extractUsername(token)).isEqualTo("atendente1");
        }
    }

    @Nested
    @DisplayName("Validação de token")
    class ValidacaoDeToken {

        @Test
        @DisplayName("Token válido gerado para o mesmo usuário deve ser aceito")
        void deveValidarTokenValido() {
            UserDetails user = userDetails("atendente1");
            String token = jwtService.generateToken(user);
            assertThat(jwtService.isTokenValid(token, user)).isTrue();
        }

        @Test
        @DisplayName("Token de outro usuário não deve ser válido")
        void naoDeveValidarTokenDeOutroUsuario() {
            String token = jwtService.generateToken(userDetails("atendente1"));
            assertThat(jwtService.isTokenValid(token, userDetails("atendente2"))).isFalse();
        }

        @Test
        @DisplayName("Token expirado não deve ser válido")
        void naoDeveValidarTokenExpirado() {
            ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
            String token = jwtService.generateToken(userDetails("atendente1"));

            ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
            assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails("atendente1")))
                    .isInstanceOf(Exception.class);
        }
    }
}
