package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UsuarioRepository - Persistência de usuários")
class UsuarioRepositoryIntegrationTest {

    @Autowired private UsuarioRepository repository;
    @Autowired private TestEntityManager entityManager;

    private Usuario salvarUsuario(String username) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword("$2a$10$hashqualquer");
        return repository.save(u);
    }

    @Nested
    @DisplayName("findByUsername")
    class FindByUsername {

        @Test
        @DisplayName("Deve encontrar usuário pelo username")
        void deveEncontrarPorUsername() {
            salvarUsuario("atendente1");
            entityManager.flush();

            Optional<Usuario> resultado = repository.findByUsername("atendente1");

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getUsername()).isEqualTo("atendente1");
        }

        @Test
        @DisplayName("Deve retornar vazio para username inexistente")
        void deveRetornarVazioParaUsernameInexistente() {
            Optional<Usuario> resultado = repository.findByUsername("nao.existe");

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByUsername")
    class ExistsByUsername {

        @Test
        @DisplayName("Deve retornar true quando username já existe")
        void deveRetornarTrueQuandoUsernameExiste() {
            salvarUsuario("atendente1");
            entityManager.flush();

            assertThat(repository.existsByUsername("atendente1")).isTrue();
        }

        @Test
        @DisplayName("Deve retornar false quando username não existe")
        void deveRetornarFalseQuandoUsernameNaoExiste() {
            assertThat(repository.existsByUsername("nao.existe")).isFalse();
        }
    }

    @Nested
    @DisplayName("Constraint de unicidade")
    class Unicidade {

        @Test
        @DisplayName("Não deve permitir dois usuários com o mesmo username")
        void naoDevePermitirUsernameDuplicado() {
            salvarUsuario("atendente1");
            entityManager.flush();

            Usuario duplicado = new Usuario();
            duplicado.setUsername("atendente1");
            duplicado.setPassword("$2a$10$outrohash");
            repository.save(duplicado);

            assertThatThrownBy(() -> entityManager.flush())
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Persistência")
    class Persistencia {

        @Test
        @DisplayName("Deve persistir todos os campos obrigatórios corretamente")
        void devePersistirCamposObrigatorios() {
            Usuario salvo = salvarUsuario("atendente1");
            entityManager.flush();
            entityManager.clear();

            Usuario recuperado = repository.findById(salvo.getId()).orElseThrow();

            assertThat(recuperado.getUsername()).isEqualTo("atendente1");
            assertThat(recuperado.getPassword()).isEqualTo("$2a$10$hashqualquer");
            assertThat(recuperado.getAtivo()).isTrue();
            assertThat(recuperado.getCriadoEm()).isNotNull();
            assertThat(recuperado.getId()).isNotNull();
        }

        @Test
        @DisplayName("Deve iniciar com ativo=true por padrão")
        void deveIniciarAtivoPorPadrao() {
            Usuario salvo = salvarUsuario("atendente1");
            entityManager.flush();

            assertThat(salvo.getAtivo()).isTrue();
        }

        @Test
        @DisplayName("isEnabled deve refletir o campo ativo")
        void isEnabledDeveRefletirAtivo() {
            Usuario ativo = salvarUsuario("ativo");
            ativo.setAtivo(true);

            Usuario inativo = salvarUsuario("inativo");
            inativo.setAtivo(false);
            entityManager.flush();

            assertThat(ativo.isEnabled()).isTrue();
            assertThat(inativo.isEnabled()).isFalse();
        }
    }
}
