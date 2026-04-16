package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Peca;
import br.com.fiap.siase.model.Servico;
import br.com.fiap.siase.model.ServicoInsumo;
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

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ServicoRepository - Integração")
class ServicoRepositoryIntegrationTest {

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private PecaRepository pecaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Servico trocaDeOleo;
    private Servico alinhamento;

    @BeforeEach
    void setUp() {
        trocaDeOleo = new Servico();
        trocaDeOleo.setNome("Troca de Óleo");
        trocaDeOleo.setDescricao("Substituição do óleo do motor");
        trocaDeOleo.setPreco(new BigDecimal("120.00"));
        trocaDeOleo.setAtivo(true);
        trocaDeOleo = servicoRepository.save(trocaDeOleo);

        alinhamento = new Servico();
        alinhamento.setNome("Alinhamento");
        alinhamento.setDescricao("Alinhamento e balanceamento");
        alinhamento.setPreco(new BigDecimal("80.00"));
        alinhamento.setAtivo(true);
        alinhamento = servicoRepository.save(alinhamento);

        entityManager.flush();
    }

    @Nested
    @DisplayName("Listagem por status ativo")
    class ListagemPorAtivo {

        @Test
        @DisplayName("Deve retornar apenas serviços ativos")
        void deveRetornarApenasAtivos() {
            Servico inativo = new Servico();
            inativo.setNome("Serviço Descontinuado");
            inativo.setPreco(BigDecimal.TEN);
            inativo.setAtivo(false);
            servicoRepository.save(inativo);
            entityManager.flush();

            List<Servico> resultado = servicoRepository.findByAtivoTrue();

            assertThat(resultado)
                    .hasSize(2)
                    .allMatch(Servico::getAtivo)
                    .extracting(Servico::getNome)
                    .containsExactlyInAnyOrder("Troca de Óleo", "Alinhamento");
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando todos os serviços estão inativos")
        void deveRetornarVazioQuandoTodosInativos() {
            trocaDeOleo.setAtivo(false);
            alinhamento.setAtivo(false);
            servicoRepository.saveAll(List.of(trocaDeOleo, alinhamento));
            entityManager.flush();

            List<Servico> resultado = servicoRepository.findByAtivoTrue();

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("Persistência de insumos vinculados ao serviço")
    class PersistenciaDeInsumos {

        @Test
        @DisplayName("Deve persistir serviço com insumos corretamente")
        void devePersistirComInsumos() {
            Peca oleo = new Peca();
            oleo.setCodigo("OL-5W30");
            oleo.setNome("Óleo 5W30");
            oleo.setPreco(new BigDecimal("45.00"));
            oleo.setQuantidadeEstoque(100);
            oleo.setUnidadeMedida("L");
            oleo.setAtivo(true);
            oleo = pecaRepository.save(oleo);

            trocaDeOleo.getInsumos().add(new ServicoInsumo(trocaDeOleo, oleo, 4));
            servicoRepository.save(trocaDeOleo);
            entityManager.flush();
            entityManager.clear();

            Servico recuperado = servicoRepository.findById(trocaDeOleo.getId()).orElseThrow();

            assertThat(recuperado.getInsumos()).hasSize(1);
            assertThat(recuperado.getInsumos().get(0).getQuantidade()).isEqualTo(4);
            assertThat(recuperado.getInsumos().get(0).getPeca().getCodigo()).isEqualTo("OL-5W30");
        }

        @Test
        @DisplayName("Deve remover insumo ao desvincular (orphanRemoval)")
        void deveRemoverInsumoAoDesvincular() {
            Peca filtro = new Peca();
            filtro.setCodigo("FLT-001");
            filtro.setNome("Filtro de Óleo");
            filtro.setPreco(new BigDecimal("25.00"));
            filtro.setQuantidadeEstoque(50);
            filtro.setUnidadeMedida("UN");
            filtro.setAtivo(true);
            filtro = pecaRepository.save(filtro);

            trocaDeOleo.getInsumos().add(new ServicoInsumo(trocaDeOleo, filtro, 1));
            servicoRepository.save(trocaDeOleo);
            entityManager.flush();

            trocaDeOleo.getInsumos().clear();
            servicoRepository.save(trocaDeOleo);
            entityManager.flush();
            entityManager.clear();

            Servico recuperado = servicoRepository.findById(trocaDeOleo.getId()).orElseThrow();
            assertThat(recuperado.getInsumos()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Persistência de preço e dados")
    class PersistenciaDeDados {

        @Test
        @DisplayName("Deve persistir corretamente o preço com precisão decimal")
        void devePersistirPrecisaoDecimal() {
            Servico servico = new Servico();
            servico.setNome("Serviço Especial");
            servico.setPreco(new BigDecimal("99.99"));
            servico.setAtivo(true);
            servicoRepository.save(servico);
            entityManager.flush();
            entityManager.clear();

            Servico recuperado = servicoRepository.findById(servico.getId()).orElseThrow();
            assertThat(recuperado.getPreco()).isEqualByComparingTo("99.99");
        }

        @Test
        @DisplayName("Deve preencher criadoEm e atualizadoEm automaticamente")
        void devePreencherAuditoria() {
            entityManager.clear();
            Servico recuperado = servicoRepository.findById(trocaDeOleo.getId()).orElseThrow();

            assertThat(recuperado.getCriadoEm()).isNotNull();
            assertThat(recuperado.getAtualizadoEm()).isNotNull();
        }
    }
}
