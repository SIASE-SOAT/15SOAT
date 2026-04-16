package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.OrdemDeServico;
import br.com.fiap.siase.model.Veiculo;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Testes de Integração - OrdemDeServicoRepository")
class OrdemDeServicoRepositoryIntegrationTest {

    @Autowired
    private OrdemDeServicoRepository ordemDeServicoRepository;

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
        cliente = new Cliente();
        cliente.setNome("Maria Santos");
        cliente.setDocumento("98765432101");
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setEmail("maria@example.com");
        cliente.setTelefone("11888888888");
        cliente = clienteRepository.save(cliente);

        veiculo = new Veiculo();
        veiculo.setCliente(cliente);
        veiculo.setPlaca("XYZ9876");
        veiculo.setMarca("Honda");
        veiculo.setModelo("Civic");
        veiculo.setAno(2022);
        veiculo = veiculoRepository.save(veiculo);
    }

    @Nested
    @DisplayName("Cenários de busca por número")
    class BuscaPorNumero {

        @Test
        @DisplayName("Deve encontrar ordem existente por número único")
        void testFindByNumeroExistence() {
            OrdemDeServico os = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-TEST-001");
            ordemDeServicoRepository.save(os);
            entityManager.flush();

            Optional<OrdemDeServico> resultado = ordemDeServicoRepository.findByNumero("OS-TEST-001");

            assertThat(resultado)
                    .isPresent()
                    .get()
                    .isEqualTo(os);
        }

        @Test
        @DisplayName("Deve encontrar com sucesso após multiplas ordens")
        void testFindByNumeroMultiplasOrdens() {
            OrdemDeServico os1 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-2026-001");
            OrdemDeServico os2 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-2026-002");
            OrdemDeServico os3 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-2026-003");

            ordemDeServicoRepository.saveAll(List.of(os1, os2, os3));
            entityManager.flush();

            Optional<OrdemDeServico> resultado = ordemDeServicoRepository.findByNumero("OS-2026-002");

            assertThat(resultado)
                    .isPresent()
                    .get()
                    .extracting(OrdemDeServico::getNumero)
                    .isEqualTo("OS-2026-002");
        }
    }

    @Nested
    @DisplayName("Cenários de busca por cliente")
    class BuscaPorCliente {

        @Test
        @DisplayName("Deve retornar todas as ordens de um cliente")
        void testFindByClienteIdMultiplasOrdens() {
            OrdemDeServico os1 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-CLI-001");
            OrdemDeServico os2 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-CLI-002");

            ordemDeServicoRepository.saveAll(List.of(os1, os2));
            entityManager.flush();

            List<OrdemDeServico> resultado = ordemDeServicoRepository.findByClienteId(cliente.getId());

            assertThat(resultado)
                    .hasSize(2)
                    .allMatch(os -> os.getCliente().getId().equals(cliente.getId()));
        }

        @Test
        @DisplayName("Deve manter isolamento entre clientes diferentes")
        void testFindByClienteIdIsolamento() {
            Cliente cliente2 = new Cliente();
            cliente2.setNome("João Other");
            cliente2.setDocumento("55555555555");
            cliente2.setTipoPessoa(TipoPessoa.PF);
            cliente2.setEmail("joao@other.com");
            cliente2.setTelefone("11777777777");
            cliente2 = clienteRepository.save(cliente2);

            UUID cliente2Id = cliente2.getId();

            Veiculo veiculo2 = new Veiculo();
            veiculo2.setCliente(cliente2);
            veiculo2.setPlaca("OTH5555");
            veiculo2.setMarca("Ford");
            veiculo2.setModelo("Focus");
            veiculo2.setAno(2021);
            veiculo2 = veiculoRepository.save(veiculo2);

            OrdemDeServico os1 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-C1-001");
            OrdemDeServico os2 = new OrdemDeServico();
            os2.setNumero("OS-C2-001");
            os2.setCliente(cliente2);
            os2.setVeiculo(veiculo2);
            os2.setStatus(StatusOS.RECEBIDA);
            os2.setDataAbertura(LocalDateTime.now());
            os2.setTotal(BigDecimal.ZERO);

            ordemDeServicoRepository.saveAll(List.of(os1, os2));
            entityManager.flush();

            List<OrdemDeServico> resultado = ordemDeServicoRepository.findByClienteId(cliente.getId());

            assertThat(resultado)
                    .hasSize(1)
                    .allMatch(os -> os.getCliente().getId().equals(cliente.getId()))
                    .noneMatch(os -> os.getCliente().getId().equals(cliente2Id));
        }
    }

    @Nested
    @DisplayName("Cenários de busca por veículo")
    class BuscaPorVeiculo {

        @Test
        @DisplayName("Deve retornar todas as ordens de um veículo")
        void testFindByVeiculoIdMultiplasOrdens() {
            OrdemDeServico os1 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-VEI-001");
            OrdemDeServico os2 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-VEI-002");

            ordemDeServicoRepository.saveAll(List.of(os1, os2));
            entityManager.flush();

            List<OrdemDeServico> resultado = ordemDeServicoRepository.findByVeiculoId(veiculo.getId());

            assertThat(resultado)
                    .hasSize(2)
                    .allMatch(os -> os.getVeiculo().getId().equals(veiculo.getId()));
        }
    }

    @Nested
    @DisplayName("Cenários de busca por status")
    class BuscaPorStatus {

        @Test
        @DisplayName("Deve filtrar ordens por cada status possível")
        void testFindByStatusTodosStatus() {
            for (StatusOS status : StatusOS.values()) {
                OrdemDeServico os = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-S-" + status.ordinal());
                os.setStatus(status);
                ordemDeServicoRepository.save(os);
            }
            entityManager.flush();

            for (StatusOS status : StatusOS.values()) {
                List<OrdemDeServico> resultado = ordemDeServicoRepository.findByStatus(status);
                assertThat(resultado)
                        .isNotEmpty()
                        .allMatch(os -> os.getStatus() == status);
            }
        }

        @Test
        @DisplayName("Deve retornar múltiplas ordens com mesmo status")
        void testFindByStatusMultiplasOrdens() {
            OrdemDeServico os1 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-DIAG-001");
            os1.setStatus(StatusOS.EM_DIAGNOSTICO);

            OrdemDeServico os2 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-DIAG-002");
            os2.setStatus(StatusOS.EM_DIAGNOSTICO);

            OrdemDeServico os3 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-REC-001");
            os3.setStatus(StatusOS.RECEBIDA);

            ordemDeServicoRepository.saveAll(List.of(os1, os2, os3));
            entityManager.flush();

            List<OrdemDeServico> resultado = ordemDeServicoRepository.findByStatus(StatusOS.EM_DIAGNOSTICO);

            assertThat(resultado)
                    .hasSize(2)
                    .allMatch(os -> os.getStatus() == StatusOS.EM_DIAGNOSTICO);
        }
    }

    @Nested
    @DisplayName("Cenários de cálculo de tempo médio")
    class CalculoTempoMedio {

        @Test
        @DisplayName("Deve calcular corretamente tempo médio com uma ordem fechada")
        void testCalcularTempoMedioUmaOrdem() {
            LocalDateTime abertura = LocalDateTime.now().minusMinutes(120);
            LocalDateTime fechamento = LocalDateTime.now();

            OrdemDeServico os = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-TEMPO-001");
            os.setDataAbertura(abertura);
            os.setDataFechamento(fechamento);
            os.setStatus(StatusOS.FINALIZADA);

            ordemDeServicoRepository.save(os);
            entityManager.flush();

            Double tempoMedio = ordemDeServicoRepository.calcularTempoMedioExecucaoMinutos();

            assertThat(tempoMedio)
                    .isNotNull()
                    .isCloseTo(120.0, offset(1.0));
        }

        @Test
        @DisplayName("Deve calcular corretamente tempo médio com múltiplas ordens")
        void testCalcularTempoMedioMultiplasOrdens() {
            LocalDateTime agora = LocalDateTime.now();

            OrdemDeServico os1 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-TEMPO-001");
            os1.setDataAbertura(agora.minusMinutes(60));
            os1.setDataFechamento(agora);
            os1.setStatus(StatusOS.FINALIZADA);

            OrdemDeServico os2 = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-TEMPO-002");
            os2.setDataAbertura(agora.minusMinutes(120));
            os2.setDataFechamento(agora);
            os2.setStatus(StatusOS.FINALIZADA);

            ordemDeServicoRepository.saveAll(List.of(os1, os2));
            entityManager.flush();

            Double tempoMedio = ordemDeServicoRepository.calcularTempoMedioExecucaoMinutos();

            assertThat(tempoMedio)
                    .isNotNull()
                    .isGreaterThan(60)
                    .isLessThan(120);
        }

        @Test
        @DisplayName("Deve ignorar ordens não finalizadas no cálculo")
        void testCalcularTempoMedioIgnoraOrdensnaoFinalizadas() {
            LocalDateTime agora = LocalDateTime.now();

            OrdemDeServico osAberta = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-TEMPO-ABERTA");
            osAberta.setDataAbertura(agora.minusMinutes(60));
            osAberta.setDataFechamento(null);
            osAberta.setStatus(StatusOS.EM_EXECUCAO);

            OrdemDeServico osFechada = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-TEMPO-FECHADA");
            osFechada.setDataAbertura(agora.minusMinutes(120));
            osFechada.setDataFechamento(agora);
            osFechada.setStatus(StatusOS.FINALIZADA);

            ordemDeServicoRepository.saveAll(List.of(osAberta, osFechada));
            entityManager.flush();

            Double tempoMedio = ordemDeServicoRepository.calcularTempoMedioExecucaoMinutos();

            assertThat(tempoMedio)
                    .isNotNull()
                    .isCloseTo(120.0, offset(1.0));
        }
    }

    @Nested
    @DisplayName("Cenários de persistência")
    class CenariosPersistencia {

        @Test
        @DisplayName("Deve persistir todas as propriedades da ordem")
        void testPersistenciaCompleta() {
            BigDecimal totalServicos = new BigDecimal("100.00");
            BigDecimal totalPecas = new BigDecimal("50.00");
            BigDecimal total = new BigDecimal("150.00");
            String observacoes = "Ordem com observações importantes";

            OrdemDeServico os = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-PERSIST-001");
            os.setTotalServicos(totalServicos);
            os.setTotalPecas(totalPecas);
            os.setTotal(total);
            os.setObservacoes(observacoes);

            OrdemDeServico saved = ordemDeServicoRepository.save(os);
            entityManager.flush();
            entityManager.clear();

            Optional<OrdemDeServico> recuperada = ordemDeServicoRepository.findById(saved.getId());

            assertThat(recuperada)
                    .isPresent()
                    .get()
                    .satisfies(o -> {
                        assertThat(o.getTotalServicos()).isEqualByComparingTo(totalServicos);
                        assertThat(o.getTotalPecas()).isEqualByComparingTo(totalPecas);
                        assertThat(o.getTotal()).isEqualByComparingTo(total);
                        assertThat(o.getObservacoes()).isEqualTo(observacoes);
                    });
        }

        @Test
        @DisplayName("Deve manter associações com Cliente e Veículo")
        void testAssociacoesMantidas() {
            OrdemDeServico os = OrdemDeServicoRepositoryIntegrationTest.this.criarOrdemDeServico("OS-ASSOC-001");

            OrdemDeServico saved = ordemDeServicoRepository.save(os);
            entityManager.flush();
            entityManager.clear();

            Optional<OrdemDeServico> recuperada = ordemDeServicoRepository.findById(saved.getId());

            assertThat(recuperada)
                    .isPresent()
                    .get()
                    .satisfies(o -> {
                        assertThat(o.getCliente()).isNotNull();
                        assertThat(o.getCliente().getId()).isEqualTo(cliente.getId());
                        assertThat(o.getVeiculo()).isNotNull();
                        assertThat(o.getVeiculo().getId()).isEqualTo(veiculo.getId());
                    });
        }
    }

    private OrdemDeServico criarOrdemDeServico(String numero) {
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
}
