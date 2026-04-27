package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.enums.TipoPessoa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ClienteRepository - Integração")
class ClienteRepositoryIntegrationTest {

    @Autowired
    private ClienteRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private Cliente joao;
    private Cliente empresa;

    @BeforeEach
    void setUp() {
        joao    = cliente("João Silva",    TipoPessoa.PF, "529.982.247-25", "joao@email.com");
        empresa = cliente("Tech LTDA",     TipoPessoa.PJ, "11.222.333/0001-81", "contato@tech.com");
        repository.saveAll(List.of(joao, empresa));
        entityManager.flush();
    }

    @Nested
    @DisplayName("findByDocumento")
    class FindByDocumento {

        @Test
        @DisplayName("Deve encontrar cliente pelo documento")
        void deveEncontrarPorDocumento() {
            Optional<Cliente> resultado = repository.findByDocumento("529.982.247-25");

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getNome()).isEqualTo("João Silva");
        }

        @Test
        @DisplayName("Deve retornar vazio para documento inexistente")
        void deveRetornarVazioParaDocumentoInexistente() {
            Optional<Cliente> resultado = repository.findByDocumento("000.000.000-00");

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByDocumento")
    class ExistsByDocumento {

        @Test
        @DisplayName("Deve retornar true para documento existente")
        void deveRetornarTrueParaDocumentoExistente() {
            assertThat(repository.existsByDocumento("529.982.247-25")).isTrue();
        }

        @Test
        @DisplayName("Deve retornar false para documento inexistente")
        void deveRetornarFalseParaDocumentoInexistente() {
            assertThat(repository.existsByDocumento("999.999.999-99")).isFalse();
        }
    }

    @Nested
    @DisplayName("findByTipoPessoa")
    class FindByTipoPessoa {

        @Test
        @DisplayName("Deve listar apenas clientes PF")
        void deveListarClientesPF() {
            List<Cliente> resultado = repository.findByTipoPessoa(TipoPessoa.PF);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNome()).isEqualTo("João Silva");
        }

        @Test
        @DisplayName("Deve listar apenas clientes PJ")
        void deveListarClientesPJ() {
            List<Cliente> resultado = repository.findByTipoPessoa(TipoPessoa.PJ);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNome()).isEqualTo("Tech LTDA");
        }
    }

    private Cliente cliente(String nome, TipoPessoa tipo, String documento, String email) {
        Cliente c = new Cliente();
        c.setNome(nome);
        c.setTipoPessoa(tipo);
        c.setDocumento(documento);
        c.setEmail(email);
        c.setAtivo(true);
        return c;
    }
}
