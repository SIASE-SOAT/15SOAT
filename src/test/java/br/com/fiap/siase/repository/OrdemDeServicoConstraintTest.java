package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.*;
import br.com.fiap.siase.model.enums.StatusOS;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integridade de Dados - Constraints e Cascade")
class OrdemDeServicoConstraintTest {

    @Autowired private OrdemDeServicoRepository ordemDeServicoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private VeiculoRepository veiculoRepository;
    @Autowired private PecaRepository pecaRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private TestEntityManager entityManager;

    private Cliente cliente;
    private Veiculo veiculo;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setNome("Ana Lima");
        cliente.setDocumento("99988877766");
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setEmail("ana.constraint@example.com");
        cliente.setTelefone("11900000099");
        cliente = clienteRepository.save(cliente);

        veiculo = new Veiculo();
        veiculo.setCliente(cliente);
        veiculo.setPlaca("CST9999");
        veiculo.setMarca("VW");
        veiculo.setModelo("Gol");
        veiculo.setAno(2019);
        veiculo = veiculoRepository.save(veiculo);

        entityManager.flush();
    }

    @Nested
    @DisplayName("Unicidade de campos")
    class Unicidade {

        @Test
        @DisplayName("Não deve permitir duas OS com o mesmo número")
        void deveLancarExcecaoNumeroOsDuplicado() {
            ordemDeServicoRepository.save(criarOS("OS-UNIQ-001"));
            entityManager.flush();

            ordemDeServicoRepository.save(criarOS("OS-UNIQ-001"));

            assertThatThrownBy(() -> entityManager.flush())
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Não deve permitir dois clientes com o mesmo documento")
        void deveLancarExcecaoDocumentoClienteDuplicado() {
            Cliente duplicado = new Cliente();
            duplicado.setNome("Outro Nome");
            duplicado.setDocumento("99988877766"); // mesmo documento do setUp
            duplicado.setTipoPessoa(TipoPessoa.PF);
            duplicado.setEmail("outro@example.com");
            duplicado.setTelefone("11900000001");
            clienteRepository.save(duplicado);

            assertThatThrownBy(() -> entityManager.flush())
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Não deve permitir dois veículos com a mesma placa")
        void deveLancarExcecaoPlacaVeiculoDuplicada() {
            Veiculo duplicado = new Veiculo();
            duplicado.setCliente(cliente);
            duplicado.setPlaca("CST9999"); // mesma placa do setUp
            duplicado.setMarca("Ford");
            duplicado.setModelo("Ka");
            duplicado.setAno(2020);
            veiculoRepository.save(duplicado);

            assertThatThrownBy(() -> entityManager.flush())
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Não deve permitir dois números de peça com o mesmo código")
        void deveLancarExcecaoCodigoPecaDuplicado() {
            Peca p1 = criarPeca("FILTRO-DUP", "Filtro de Óleo");
            Peca p2 = criarPeca("FILTRO-DUP", "Filtro de Ar"); // mesmo código
            pecaRepository.save(p1);
            entityManager.flush();

            pecaRepository.save(p2);

            assertThatThrownBy(() -> entityManager.flush())
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Cascade ao deletar OS")
    class CascadeDelete {

        @Test
        @DisplayName("Deve remover ItemServico ao deletar a OS")
        void deveCascadearDeletacaoDeItensServico() {
            Servico servico = new Servico();
            servico.setNome("Troca de Óleo");
            servico.setPreco(new BigDecimal("80.00"));
            servico.setAtivo(true);
            servico = servicoRepository.save(servico);

            OrdemDeServico os = ordemDeServicoRepository.save(criarOS("OS-CASC-SVC"));

            ItemServico item = new ItemServico();
            item.setOrdemDeServico(os);
            item.setServico(servico);
            item.setPrecoUnitario(new BigDecimal("80.00"));
            os.getItensServico().add(item);
            entityManager.flush();

            UUID itemId = os.getItensServico().get(0).getId();
            assertThat(entityManager.find(ItemServico.class, itemId)).isNotNull();

            ordemDeServicoRepository.delete(os);
            entityManager.flush();
            entityManager.clear();

            assertThat(entityManager.find(ItemServico.class, itemId)).isNull();
        }

        @Test
        @DisplayName("Deve remover ItemPeca ao deletar a OS")
        void deveCascadearDeletacaoDeItensPeca() {
            Peca peca = criarPeca("FILTRO-CASC", "Filtro Cascata");
            peca = pecaRepository.save(peca);

            OrdemDeServico os = ordemDeServicoRepository.save(criarOS("OS-CASC-PCA"));

            ItemPeca item = new ItemPeca();
            item.setOrdemDeServico(os);
            item.setPeca(peca);
            item.setQuantidade(2);
            item.setPrecoUnitario(new BigDecimal("25.00"));
            os.getItensPeca().add(item);
            entityManager.flush();

            UUID itemId = os.getItensPeca().get(0).getId();
            assertThat(entityManager.find(ItemPeca.class, itemId)).isNotNull();

            ordemDeServicoRepository.delete(os);
            entityManager.flush();
            entityManager.clear();

            assertThat(entityManager.find(ItemPeca.class, itemId)).isNull();
        }

        @Test
        @DisplayName("Deve remover todos os itens de uma OS com múltiplos serviços e peças")
        void deveCascadearTodosOsItens() {
            Servico s1 = new Servico();
            s1.setNome("Alinhamento");
            s1.setPreco(new BigDecimal("60.00"));
            s1.setAtivo(true);
            s1 = servicoRepository.save(s1);

            Servico s2 = new Servico();
            s2.setNome("Balanceamento");
            s2.setPreco(new BigDecimal("40.00"));
            s2.setAtivo(true);
            s2 = servicoRepository.save(s2);

            Peca peca = criarPeca("PNEU-001", "Pneu");
            peca = pecaRepository.save(peca);

            OrdemDeServico os = ordemDeServicoRepository.save(criarOS("OS-CASC-MULTI"));

            ItemServico item1 = new ItemServico();
            item1.setOrdemDeServico(os);
            item1.setServico(s1);
            item1.setPrecoUnitario(new BigDecimal("60.00"));
            os.getItensServico().add(item1);

            ItemServico item2 = new ItemServico();
            item2.setOrdemDeServico(os);
            item2.setServico(s2);
            item2.setPrecoUnitario(new BigDecimal("40.00"));
            os.getItensServico().add(item2);

            ItemPeca itemPeca = new ItemPeca();
            itemPeca.setOrdemDeServico(os);
            itemPeca.setPeca(peca);
            itemPeca.setQuantidade(4);
            itemPeca.setPrecoUnitario(new BigDecimal("300.00"));
            os.getItensPeca().add(itemPeca);

            entityManager.flush();

            UUID osId = os.getId();
            ordemDeServicoRepository.delete(os);
            entityManager.flush();
            entityManager.clear();

            assertThat(entityManager.find(OrdemDeServico.class, osId)).isNull();
            assertThat(entityManager
                    .getEntityManager()
                    .createQuery("SELECT COUNT(i) FROM ItemServico i WHERE i.ordemDeServico.id = :id", Long.class)
                    .setParameter("id", osId)
                    .getSingleResult()).isZero();
            assertThat(entityManager
                    .getEntityManager()
                    .createQuery("SELECT COUNT(i) FROM ItemPeca i WHERE i.ordemDeServico.id = :id", Long.class)
                    .setParameter("id", osId)
                    .getSingleResult()).isZero();
        }
    }

    private OrdemDeServico criarOS(String numero) {
        OrdemDeServico os = new OrdemDeServico();
        os.setNumero(numero);
        os.setCliente(cliente);
        os.setVeiculo(veiculo);
        os.setStatus(StatusOS.RECEBIDA);
        os.setDataAbertura(LocalDateTime.now());
        os.setTotal(BigDecimal.ZERO);
        os.setTotalServicos(BigDecimal.ZERO);
        os.setTotalPecas(BigDecimal.ZERO);
        return os;
    }

    private Peca criarPeca(String codigo, String nome) {
        Peca peca = new Peca();
        peca.setCodigo(codigo);
        peca.setNome(nome);
        peca.setPreco(new BigDecimal("25.00"));
        peca.setQuantidadeEstoque(10);
        peca.setUnidadeMedida("UN");
        return peca;
    }
}
