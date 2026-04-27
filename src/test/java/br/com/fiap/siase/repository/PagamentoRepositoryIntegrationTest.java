package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.*;
import br.com.fiap.siase.model.enums.FormaPagamento;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PagamentoRepository - Integração")
class PagamentoRepositoryIntegrationTest {

    @Autowired
    private PagamentoRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private OrdemDeServico os;
    private Pagamento pagamento;

    @BeforeEach
    void setUp() {
        Cliente cliente = new Cliente();
        cliente.setNome("João Silva");
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setDocumento("529.982.247-25");
        cliente.setAtivo(true);
        entityManager.persistAndFlush(cliente);

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca("ABC1234");
        veiculo.setMarca("Toyota");
        veiculo.setModelo("Corolla");
        veiculo.setAno(2022);
        veiculo.setAtivo(true);
        veiculo.setCliente(cliente);
        entityManager.persistAndFlush(veiculo);

        os = new OrdemDeServico();
        os.setNumero("OS-20260427-TEST1");
        os.setCliente(cliente);
        os.setVeiculo(veiculo);
        entityManager.persistAndFlush(os);

        pagamento = new Pagamento();
        pagamento.setOrdemDeServico(os);
        pagamento.setFormaPagamento(FormaPagamento.PIX);
        pagamento.setValor(new BigDecimal("350.00"));
        repository.saveAndFlush(pagamento);
    }

    @Nested
    @DisplayName("findByOrdemDeServicoId")
    class FindByOrdemDeServicoId {

        @Test
        @DisplayName("Deve encontrar pagamento pela OS")
        void deveEncontrarPagamentoPelaOS() {
            Optional<Pagamento> resultado = repository.findByOrdemDeServicoId(os.getId());

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getValor()).isEqualByComparingTo("350.00");
        }

        @Test
        @DisplayName("Deve retornar vazio para OS sem pagamento")
        void deveRetornarVazioParaOsSemPagamento() {
            Optional<Pagamento> resultado = repository.findByOrdemDeServicoId(UUID.randomUUID());

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByOrdemDeServicoId")
    class ExistsByOrdemDeServicoId {

        @Test
        @DisplayName("Deve retornar true para OS com pagamento")
        void deveRetornarTrueParaOsComPagamento() {
            assertThat(repository.existsByOrdemDeServicoId(os.getId())).isTrue();
        }

        @Test
        @DisplayName("Deve retornar false para OS sem pagamento")
        void deveRetornarFalseParaOsSemPagamento() {
            assertThat(repository.existsByOrdemDeServicoId(UUID.randomUUID())).isFalse();
        }
    }
}
