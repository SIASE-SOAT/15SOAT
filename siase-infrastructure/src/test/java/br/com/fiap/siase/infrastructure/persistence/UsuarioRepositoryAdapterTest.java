package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Usuario;
import br.com.fiap.siase.domain.port.UsuarioRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("UsuarioRepositoryAdapter - Testes de integração com persistência")
class UsuarioRepositoryAdapterTest {

    @Autowired
    UsuarioRepositoryPort adapter;

    private Usuario criarUsuario(String username) {
        return Usuario.builder()
                .username(username)
                .password("$2a$10$hashedPasswordExample1234567890AB")
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("deve salvar e recuperar usuário por username")
    void deveSalvarERecuperarUsuarioPorUsername() {
        Usuario usuario = criarUsuario("joao.silva");

        Usuario salvo = adapter.save(usuario);

        assertThat(salvo.getId()).isNotNull();

        Optional<Usuario> encontrado = adapter.findByUsername("joao.silva");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsername()).isEqualTo("joao.silva");
        assertThat(encontrado.get().getAtivo()).isTrue();
        assertThat(encontrado.get().getCriadoEm()).isNotNull();
    }

    @Test
    @DisplayName("deve verificar existência de username: true para cadastrado, false para inexistente")
    void deveVerificarExistenciaDeUsername() {
        adapter.save(criarUsuario("maria.santos"));

        boolean existente = adapter.existsByUsername("maria.santos");
        boolean naoExistente = adapter.existsByUsername("username-inexistente-" + UUID.randomUUID());

        assertThat(existente).isTrue();
        assertThat(naoExistente).isFalse();
    }

    @Test
    @DisplayName("deve retornar vazio quando usuário não existe pelo username")
    void deveRetornarVazioQuandoUsuarioNaoExiste() {
        Optional<Usuario> resultado = adapter.findByUsername("usuario-que-nao-existe-" + UUID.randomUUID());

        assertThat(resultado).isEmpty();
    }
}
