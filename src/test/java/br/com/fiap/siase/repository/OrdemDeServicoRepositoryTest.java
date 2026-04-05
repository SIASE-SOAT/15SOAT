package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.OrdemDeServico;
import br.com.fiap.siase.model.Veiculo;
import br.com.fiap.siase.model.enums.StatusOS;
import br.com.fiap.siase.model.enums.TipoPessoa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("Testes Unitários - OrdemDeServicoRepository")
class OrdemDeServicoRepositoryTest {

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
    private OrdemDeServico ordemDeServico;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setNome("João Silva");
        cliente.setDocumento("12345678901");
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setEmail("joao@example.com");
        cliente.setTelefone("11999999999");
        cliente = clienteRepository.save(cliente);

        veiculo = new Veiculo();
        veiculo.setCliente(cliente);
        veiculo.setPlaca("ABC1234");
        veiculo.setMarca("Toyota");
        veiculo.setModelo("Corolla");
        veiculo.setAno(2020);
        veiculo = veiculoRepository.save(veiculo);

        ordemDeServico = new OrdemDeServico();
        ordemDeServico.setNumero("OS-2026-0001");
        ordemDeServico.setCliente(cliente);
        ordemDeServico.setVeiculo(veiculo);
        ordemDeServico.setStatus(StatusOS.RECEBIDA);
        ordemDeServico.setDataAbertura(LocalDateTime.now());
        ordemDeServico.setTotal(BigDecimal.ZERO);
        ordemDeServico.setTotalServicos(BigDecimal.ZERO);
        ordemDeServico.setTotalPecas(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve encontrar OrdemDeServico por número")
    void testFindByNumero() {
        ordemDeServicoRepository.save(ordemDeServico);
        entityManager.flush();

        Optional<OrdemDeServico> resultado = ordemDeServicoRepository.findByNumero("OS-2026-0001");

        assertThat(resultado)
                .isPresent()
                .get()
                .extracting(OrdemDeServico::getNumero)
                .isEqualTo("OS-2026-0001");
    }

    @Test
    @DisplayName("Deve retornar vazio quando número não existe")
    void testFindByNumeroNotFound() {
        Optional<OrdemDeServico> resultado = ordemDeServicoRepository.findByNumero("OS-9999-9999");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve encontrar OrdemDeServico por ID do cliente")
    void testFindByClienteId() {
        OrdemDeServico os1 = new OrdemDeServico();
        os1.setNumero("OS-2026-0001");
        os1.setCliente(cliente);
        os1.setVeiculo(veiculo);
        os1.setStatus(StatusOS.RECEBIDA);
        os1.setDataAbertura(LocalDateTime.now());
        os1.setTotal(BigDecimal.ZERO);

        OrdemDeServico os2 = new OrdemDeServico();
        os2.setNumero("OS-2026-0002");
        os2.setCliente(cliente);
        os2.setVeiculo(veiculo);
        os2.setStatus(StatusOS.EM_DIAGNOSTICO);
        os2.setDataAbertura(LocalDateTime.now());
        os2.setTotal(BigDecimal.ZERO);

        ordemDeServicoRepository.save(os1);
        ordemDeServicoRepository.save(os2);
        entityManager.flush();

        List<OrdemDeServico> resultado = ordemDeServicoRepository.findByClienteId(cliente.getId());

        assertThat(resultado)
                .hasSize(2)
                .extracting(OrdemDeServico::getNumero)
                .contains("OS-2026-0001", "OS-2026-0002");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando cliente não tem ordens")
    void testFindByClienteIdEmpty() {
        UUID clienteIdInexistente = UUID.randomUUID();

        List<OrdemDeServico> resultado = ordemDeServicoRepository.findByClienteId(clienteIdInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve encontrar OrdemDeServico por ID do veículo")
    void testFindByVeiculoId() {
        OrdemDeServico os1 = new OrdemDeServico();
        os1.setNumero("OS-2026-0001");
        os1.setCliente(cliente);
        os1.setVeiculo(veiculo);
        os1.setStatus(StatusOS.RECEBIDA);
        os1.setDataAbertura(LocalDateTime.now());
        os1.setTotal(BigDecimal.ZERO);

        ordemDeServicoRepository.save(os1);
        entityManager.flush();

        List<OrdemDeServico> resultado = ordemDeServicoRepository.findByVeiculoId(veiculo.getId());

        assertThat(resultado)
                .hasSize(1)
                .extracting(OrdemDeServico::getNumero)
                .contains("OS-2026-0001");
    }

    @Test
    @DisplayName("Deve encontrar OrdemDeServico por status")
    void testFindByStatus() {
        OrdemDeServico os1 = new OrdemDeServico();
        os1.setNumero("OS-S-001");
        os1.setCliente(cliente);
        os1.setVeiculo(veiculo);
        os1.setStatus(StatusOS.RECEBIDA);
        os1.setDataAbertura(LocalDateTime.now());
        os1.setTotal(BigDecimal.ZERO);

        OrdemDeServico os2 = new OrdemDeServico();
        os2.setNumero("OS-2026-0002");
        os2.setCliente(cliente);
        os2.setVeiculo(veiculo);
        os2.setStatus(StatusOS.EM_DIAGNOSTICO);
        os2.setDataAbertura(LocalDateTime.now());
        os2.setTotal(BigDecimal.ZERO);

        ordemDeServicoRepository.save(os1);
        ordemDeServicoRepository.save(os2);
        entityManager.flush();

        List<OrdemDeServico> resultado = ordemDeServicoRepository.findByStatus(StatusOS.RECEBIDA);

        assertThat(resultado)
                .hasSize(1)
                .extracting(OrdemDeServico::getStatus)
                .containsOnly(StatusOS.RECEBIDA);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando nenhuma ordem tem status especificado")
    void testFindByStatusEmpty() {
        OrdemDeServico os1 = new OrdemDeServico();
        os1.setNumero("OS-S-003");
        os1.setCliente(cliente);
        os1.setVeiculo(veiculo);
        os1.setStatus(StatusOS.RECEBIDA);
        os1.setDataAbertura(LocalDateTime.now());
        os1.setTotal(BigDecimal.ZERO);

        ordemDeServicoRepository.save(os1);
        entityManager.flush();

        List<OrdemDeServico> resultado = ordemDeServicoRepository.findByStatus(StatusOS.ENTREGUE);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve calcular tempo médio de execução em minutos")
    void testCalcularTempoMedioExecucaoMinutos() {
        LocalDateTime dataAbertura = LocalDateTime.now().minusMinutes(60);
        LocalDateTime dataFechamento = LocalDateTime.now();

        OrdemDeServico os1 = new OrdemDeServico();
        os1.setNumero("OS-T-001");
        os1.setCliente(cliente);
        os1.setVeiculo(veiculo);
        os1.setStatus(StatusOS.FINALIZADA);
        os1.setDataAbertura(dataAbertura);
        os1.setDataFechamento(dataFechamento);
        os1.setTotal(BigDecimal.ZERO);

        ordemDeServicoRepository.save(os1);
        entityManager.flush();

        Double tempoMedio = ordemDeServicoRepository.calcularTempoMedioExecucaoMinutos();

        assertThat(tempoMedio)
                .isNotNull()
                .isGreaterThan(0)
                .isLessThanOrEqualTo(60);
    }

    @Test
    @DisplayName("Deve retornar null quando nenhuma ordem foi fechada")
    void testCalcularTempoMedioExecucaoMinutosNoPecas() {
        OrdemDeServico os1 = new OrdemDeServico();
        os1.setNumero("OS-T-002");
        os1.setCliente(cliente);
        os1.setVeiculo(veiculo);
        os1.setStatus(StatusOS.RECEBIDA);
        os1.setDataAbertura(LocalDateTime.now());
        os1.setDataFechamento(null);
        os1.setTotal(BigDecimal.ZERO);

        ordemDeServicoRepository.save(os1);
        entityManager.flush();

        Double tempoMedio = ordemDeServicoRepository.calcularTempoMedioExecucaoMinutos();

        assertThat(tempoMedio).isNull();
    }

    @Test
    @DisplayName("Deve salvar e recuperar OrdemDeServico com sucesso")
    void testSaveAndFind() {
        ordemDeServicoRepository.save(ordemDeServico);
        entityManager.flush();
        UUID savedId = ordemDeServico.getId();

        Optional<OrdemDeServico> resultado = ordemDeServicoRepository.findById(savedId);

        assertThat(resultado)
                .isPresent()
                .get()
                .satisfies(os -> {
                    assertThat(os.getNumero()).isEqualTo("OS-2026-0001");
                    assertThat(os.getStatus()).isEqualTo(StatusOS.RECEBIDA);
                    assertThat(os.getCliente().getId()).isEqualTo(cliente.getId());
                    assertThat(os.getVeiculo().getId()).isEqualTo(veiculo.getId());
                });
    }

    @Test
    @DisplayName("Deve atualizar OrdemDeServico com sucesso")
    void testUpdate() {
        OrdemDeServico saved = ordemDeServicoRepository.save(ordemDeServico);
        entityManager.flush();

        saved.setStatus(StatusOS.FINALIZADA);
        saved.setDataFechamento(LocalDateTime.now());
        OrdemDeServico updated = ordemDeServicoRepository.save(saved);
        entityManager.flush();

        Optional<OrdemDeServico> resultado = ordemDeServicoRepository.findById(updated.getId());
        assertThat(resultado)
                .isPresent()
                .get()
                .extracting(OrdemDeServico::getStatus)
                .isEqualTo(StatusOS.FINALIZADA);
    }

    @Test
    @DisplayName("Deve deletar OrdemDeServico com sucesso")
    void testDelete() {
        OrdemDeServico saved = ordemDeServicoRepository.save(ordemDeServico);
        entityManager.flush();
        UUID savedId = saved.getId();

        ordemDeServicoRepository.deleteById(savedId);
        entityManager.flush();

        Optional<OrdemDeServico> resultado = ordemDeServicoRepository.findById(savedId);
        assertThat(resultado).isEmpty();
    }
}
