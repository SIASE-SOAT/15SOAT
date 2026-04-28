package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Peca;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PecaRepository - Integração")
class PecaRepositoryIntegrationTest {

    @Autowired
    private PecaRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private Peca filtroDeOleo;
    private Peca vela;

    @BeforeEach
    void setUp() {
        filtroDeOleo = pecaCom("FILTRO-001", "Filtro de Óleo", 50, 10, true);
        vela = pecaCom("VELA-001", "Vela de Ignição", 30, 5, true);
        repository.saveAll(List.of(filtroDeOleo, vela));
        entityManager.flush();
    }

    @Nested
    @DisplayName("Listagem por status ativo")
    class ListagemPorAtivo {

        @Test
        @DisplayName("Deve retornar apenas peças ativas")
        void deveRetornarApenasAtivas() {
            Peca inativa = pecaCom("INATIVA-001", "Peça Descontinuada", 0, 0, false);
            repository.save(inativa);
            entityManager.flush();

            List<Peca> resultado = repository.findByAtivoTrue();

            assertThat(resultado)
                    .hasSize(2)
                    .allMatch(Peca::getAtivo)
                    .extracting(Peca::getCodigo)
                    .containsExactlyInAnyOrder("FILTRO-001", "VELA-001");
        }
    }

    @Nested
    @DisplayName("Busca por código")
    class BuscaPorCodigo {

        @Test
        @DisplayName("Deve encontrar peça pelo código exato")
        void deveEncontrarPorCodigo() {
            Optional<Peca> resultado = repository.findByCodigo("FILTRO-001");

            assertThat(resultado)
                    .isPresent()
                    .get()
                    .extracting(Peca::getNome)
                    .isEqualTo("Filtro de Óleo");
        }

        @Test
        @DisplayName("Deve retornar vazio para código inexistente")
        void deveRetornarVazioParaCodigoInexistente() {
            Optional<Peca> resultado = repository.findByCodigo("INEXISTENTE-999");

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve garantir unicidade do código")
        void deveGarantirUnicidadeDoCodigo() {
            Peca duplicado = pecaCom("FILTRO-001", "Filtro Duplicado", 10, 0, true);

            assertThatThrownBy(() -> {
                repository.save(duplicado);
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Controle de estoque baixo")
    class ControlDeEstoqueBaixo {

        @Test
        @DisplayName("Deve identificar peças com estoque abaixo do mínimo")
        void deveIdentificarEstoqueBaixo() {
            filtroDeOleo.setQuantidadeEstoque(5);
            filtroDeOleo.setEstoqueMinimo(10);
            repository.save(filtroDeOleo);
            entityManager.flush();

            List<Peca> resultado = repository.findEstoqueBaixo();

            assertThat(resultado)
                    .hasSize(1)
                    .extracting(Peca::getCodigo)
                    .containsExactly("FILTRO-001");
        }

        @Test
        @DisplayName("Não deve listar peça quando estoque está exatamente no mínimo")
        void naoDeveListarQuandoEstoqueIgualAoMinimo() {
            filtroDeOleo.setQuantidadeEstoque(10);
            filtroDeOleo.setEstoqueMinimo(10);
            repository.save(filtroDeOleo);
            entityManager.flush();

            // query usa < (estritamente abaixo), então estoque == mínimo não é retornado
            List<Peca> resultado = repository.findEstoqueBaixo();

            // ambas as peças têm estoque >= mínimo, então a lista deve ser vazia
            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando todos têm estoque adequado")
        void deveRetornarVazioQuandoEstoqueAdequado() {
            List<Peca> resultado = repository.findEstoqueBaixo();

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Não deve listar peças inativas com estoque baixo")
        void naoDeveListarInativasComEstoqueBaixo() {
            Peca inativa    = pecaCom("INATIVA-001",   "Peça Inativa",            0, 10, false);
            Peca ativaLow   = pecaCom("ATIVA-LOW-001", "Peça Ativa Estoque Baixo", 2, 10, true);
            repository.saveAll(List.of(inativa, ativaLow));
            entityManager.flush();

            List<Peca> resultado = repository.findEstoqueBaixo();

            // ativaLow deve aparecer; inativa deve ser filtrada mesmo com estoque baixo
            assertThat(resultado).isNotEmpty();
            assertThat(resultado)
                    .extracting(Peca::getCodigo)
                    .contains("ATIVA-LOW-001")
                    .doesNotContain("INATIVA-001");
        }
    }

    @Nested
    @DisplayName("Persistência de dados")
    class PersistenciaDeDados {

        @Test
        @DisplayName("Deve persistir todos os campos corretamente")
        void devePersistirTodosCampos() {
            Peca nova = new Peca();
            nova.setCodigo("OL-5W30");
            nova.setNome("Óleo Sintético 5W30");
            nova.setDescricao("Óleo de alta performance");
            nova.setPreco(new BigDecimal("89.90"));
            nova.setQuantidadeEstoque(200);
            nova.setEstoqueMinimo(20);
            nova.setUnidadeMedida("L");
            nova.setAtivo(true);
            repository.save(nova);
            entityManager.flush();
            entityManager.clear();

            Peca recuperada = repository.findById(nova.getId()).orElseThrow();

            assertThat(recuperada.getCodigo()).isEqualTo("OL-5W30");
            assertThat(recuperada.getPreco()).isEqualByComparingTo("89.90");
            assertThat(recuperada.getQuantidadeEstoque()).isEqualTo(200);
            assertThat(recuperada.getUnidadeMedida()).isEqualTo("L");
            assertThat(recuperada.getCriadoEm()).isNotNull();
            assertThat(recuperada.getAtualizadoEm()).isNotNull();
        }
    }

    private Peca pecaCom(String codigo, String nome, int estoque, int estoqueMinimo, boolean ativo) {
        Peca p = new Peca();
        p.setCodigo(codigo);
        p.setNome(nome);
        p.setPreco(new BigDecimal("10.00"));
        p.setQuantidadeEstoque(estoque);
        p.setEstoqueMinimo(estoqueMinimo);
        p.setUnidadeMedida("UN");
        p.setAtivo(ativo);
        return p;
    }
}
