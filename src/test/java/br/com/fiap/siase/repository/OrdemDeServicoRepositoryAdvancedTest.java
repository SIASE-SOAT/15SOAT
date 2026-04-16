package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.OrdemDeServico;
import br.com.fiap.siase.model.Veiculo;
import br.com.fiap.siase.model.enums.StatusOS;
import br.com.fiap.siase.repository.testdata.OrdemDeServicoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("OrdemDeServicoRepository - Testes Unitários em Produção")
class OrdemDeServicoRepositoryAdvancedTest {

    @Autowired
    private OrdemDeServicoRepository repository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Cliente cliente;
    private Veiculo veiculo;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(OrdemDeServicoTestData.criarCliente());
        veiculo = veiculoRepository.save(OrdemDeServicoTestData.criarVeiculo(cliente));
    }

    @Test
    @DisplayName("Deve encontrar ordem por número único")
    void findByNumero_ExistingNumber_ReturnsOrder() {
        var ordem = OrdemDeServicoTestData.builder()
                .numero("OS-UNIQUE-001")
                .cliente(cliente)
                .veiculo(veiculo)
                .build();
        repository.save(ordem);
        entityManager.flush();

        Optional<OrdemDeServico> resultado = repository.findByNumero("OS-UNIQUE-001");

        assertThat(resultado)
                .isPresent()
                .get()
                .extracting(OrdemDeServico::getNumero)
                .isEqualTo("OS-UNIQUE-001");
    }

    @Test
    @DisplayName("Deve retornar vazio para número inexistente")
    void findByNumero_NonExistingNumber_ReturnsEmpty() {
        Optional<OrdemDeServico> resultado = repository.findByNumero("OS-INEXISTENTE");

        assertThat(resultado).isEmpty();
    }

    @ParameterizedTest(name = "Status: {0}")
    @EnumSource(StatusOS.class)
    @DisplayName("Deve filtrar por cada status possível")
    void findByStatus_AllStatuses_FiltersCorrectly(StatusOS status) {
        var ordem = OrdemDeServicoTestData.builder()
                .numero("OS-S-" + status.ordinal())
                .cliente(cliente)
                .veiculo(veiculo)
                .status(status)
                .build();
        repository.save(ordem);
        entityManager.flush();

        List<OrdemDeServico> resultado = repository.findByStatus(status);

        assertThat(resultado)
                .isNotEmpty()
                .allMatch(os -> os.getStatus() == status);
    }

    @ParameterizedTest(name = "Precisão: {0}ms")
    @ValueSource(ints = {60, 120, 180, 240})
    @DisplayName("Deve calcular tempo médio com diferentes durações")
    void calcularTempoMedio_VariousDurations_CalculatesCorrectly(int minutos) {
        LocalDateTime agora = LocalDateTime.now();
        var ordem = OrdemDeServicoTestData.builder()
                .numero("OS-TEMPO-" + minutos)
                .cliente(cliente)
                .veiculo(veiculo)
                .dataAbertura(agora.minusMinutes(minutos))
                .dataFechamento(agora)
                .status(StatusOS.FINALIZADA)
                .build();
        repository.save(ordem);
        entityManager.flush();

        Double tempoMedio = repository.calcularTempoMedioExecucaoMinutos();

        assertThat(tempoMedio)
                .isNotNull()
                .isCloseTo((double) minutos, offset(1.0));
    }

    @Test
    @DisplayName("Deve retornar todas as ordens de um cliente")
    void findByClienteId_MultipleOrders_ReturnsAll() {
        var ordem1 = OrdemDeServicoTestData.builder()
                .numero("OS-CLI-001")
                .cliente(cliente)
                .veiculo(veiculo)
                .build();
        var ordem2 = OrdemDeServicoTestData.builder()
                .numero("OS-CLI-002")
                .cliente(cliente)
                .veiculo(veiculo)
                .build();

        repository.saveAll(List.of(ordem1, ordem2));
        entityManager.flush();

        List<OrdemDeServico> resultado = repository.findByClienteId(cliente.getId());

        assertThat(resultado)
                .hasSize(2)
                .allMatch(os -> os.getCliente().getId().equals(cliente.getId()));
    }

    @Test
    @DisplayName("Deve persistir totais corretamente")
    void save_WithTotals_PersistsCorrectly() {
        BigDecimal totalServicos = new BigDecimal("100.00");
        BigDecimal totalPecas = new BigDecimal("50.00");
        BigDecimal total = new BigDecimal("150.00");

        var ordem = OrdemDeServicoTestData.builder()
                .numero("OS-TOTAIS")
                .cliente(cliente)
                .veiculo(veiculo)
                .totalServicos(totalServicos)
                .totalPecas(totalPecas)
                .total(total)
                .build();

        OrdemDeServico saved = repository.save(ordem);
        entityManager.flush();
        entityManager.clear();

        Optional<OrdemDeServico> recuperada = repository.findById(saved.getId());
        assertThat(recuperada)
                .isPresent()
                .get()
                .satisfies(os -> {
                    assertThat(os.getTotalServicos()).isEqualByComparingTo(totalServicos);
                    assertThat(os.getTotalPecas()).isEqualByComparingTo(totalPecas);
                    assertThat(os.getTotal()).isEqualByComparingTo(total);
                });
    }

    @Test
    @DisplayName("Deve manter integridade de relacionamentos")
    void save_WithAssociations_MaintainsRelationships() {
        var ordem = OrdemDeServicoTestData.builder()
                .numero("OS-ASSOC")
                .cliente(cliente)
                .veiculo(veiculo)
                .build();

        OrdemDeServico saved = repository.save(ordem);
        entityManager.flush();
        entityManager.clear();

        Optional<OrdemDeServico> recuperada = repository.findById(saved.getId());
        assertThat(recuperada)
                .isPresent()
                .get()
                .satisfies(os -> {
                    assertThat(os.getCliente()).isNotNull();
                    assertThat(os.getCliente().getId()).isEqualTo(cliente.getId());
                    assertThat(os.getVeiculo()).isNotNull();
                    assertThat(os.getVeiculo().getId()).isEqualTo(veiculo.getId());
                });
    }
}
