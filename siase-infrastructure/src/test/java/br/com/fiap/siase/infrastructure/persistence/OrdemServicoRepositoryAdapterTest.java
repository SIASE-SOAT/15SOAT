package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.enums.TipoPessoa;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("OrdemServicoRepositoryAdapter - Integração com banco de dados")
class OrdemServicoRepositoryAdapterTest {

    @Autowired
    private ClienteRepositoryPort clienteRepository;

    @Autowired
    private VeiculoRepositoryPort veiculoRepository;

    @Autowired
    private OrdemServicoRepositoryPort ordemServicoRepository;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private Cliente salvarCliente() {
        int n = COUNTER.incrementAndGet();
        Cliente cliente = Cliente.builder()
                .nome("Cliente Teste " + n)
                .tipoPessoa(TipoPessoa.PF)
                .documento(String.format("%011d", n))
                .email("cliente" + n + "@example.com")
                .ativo(true)
                .build();
        return clienteRepository.save(cliente);
    }

    private Veiculo salvarVeiculo(Cliente cliente) {
        int n = COUNTER.incrementAndGet();
        String placaSuffix = String.format("%03d", n % 1000);
        Veiculo veiculo = Veiculo.builder()
                .placa("TST" + placaSuffix + "A" + (n % 10))
                .marca("Toyota")
                .modelo("Corolla")
                .ano(2022)
                .cor("Branco")
                .ativo(true)
                .cliente(cliente)
                .build();
        return veiculoRepository.save(veiculo);
    }

    private OrdemDeServico criarOS(String numero, StatusOS status) {
        Cliente cliente = salvarCliente();
        Veiculo veiculo = salvarVeiculo(cliente);
        return OrdemDeServico.builder()
                .numero(numero)
                .status(status)
                .cliente(cliente)
                .veiculo(veiculo)
                .observacoes("Observação de teste")
                .dataAbertura(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("deve salvar e recuperar OS por ID")
    void deveSalvarERecuperarOSPorId() {
        OrdemDeServico os = criarOS("OS-20260604-AA0001", StatusOS.RECEBIDA);

        OrdemDeServico salva = ordemServicoRepository.save(os);

        assertThat(salva.getId()).isNotNull();

        Optional<OrdemDeServico> encontrada = ordemServicoRepository.findById(salva.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getNumero()).isEqualTo("OS-20260604-AA0001");
        assertThat(encontrada.get().getStatus()).isEqualTo(StatusOS.RECEBIDA);
    }

    @Test
    @DisplayName("deve buscar OS por número")
    void deveBuscarOSPorNumero() {
        OrdemDeServico os = criarOS("OS-20260604-BB0002", StatusOS.RECEBIDA);
        ordemServicoRepository.save(os);

        Optional<OrdemDeServico> encontrada = ordemServicoRepository.findByNumero("OS-20260604-BB0002");

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getNumero()).isEqualTo("OS-20260604-BB0002");
    }

    @Test
    @DisplayName("deve buscar OS por ID do cliente")
    void deveBuscarOSPorClienteId() {
        Cliente cliente = salvarCliente();
        Veiculo veiculo = salvarVeiculo(cliente);

        OrdemDeServico os1 = OrdemDeServico.builder()
                .numero("OS-20260604-CC0003")
                .status(StatusOS.RECEBIDA)
                .cliente(cliente)
                .veiculo(veiculo)
                .dataAbertura(LocalDateTime.now())
                .build();

        Veiculo veiculo2 = salvarVeiculo(cliente);
        OrdemDeServico os2 = OrdemDeServico.builder()
                .numero("OS-20260604-CC0004")
                .status(StatusOS.EM_DIAGNOSTICO)
                .cliente(cliente)
                .veiculo(veiculo2)
                .dataAbertura(LocalDateTime.now())
                .build();

        ordemServicoRepository.save(os1);
        ordemServicoRepository.save(os2);

        List<OrdemDeServico> ordens = ordemServicoRepository.findByClienteId(cliente.getId());

        assertThat(ordens).hasSize(2);
        assertThat(ordens).extracting(OrdemDeServico::getNumero)
                .containsExactlyInAnyOrder("OS-20260604-CC0003", "OS-20260604-CC0004");
    }

    @Test
    @DisplayName("deve buscar OS por status")
    void deveBuscarOSPorStatus() {
        OrdemDeServico osRecebida = criarOS("OS-20260604-DD0005", StatusOS.RECEBIDA);
        OrdemDeServico osDiagnostico = criarOS("OS-20260604-DD0006", StatusOS.EM_DIAGNOSTICO);

        ordemServicoRepository.save(osRecebida);
        ordemServicoRepository.save(osDiagnostico);

        List<OrdemDeServico> resultado = ordemServicoRepository.findByStatus(StatusOS.EM_DIAGNOSTICO);

        assertThat(resultado).isNotEmpty();
        assertThat(resultado).extracting(OrdemDeServico::getNumero)
                .contains("OS-20260604-DD0006");
        assertThat(resultado).extracting(OrdemDeServico::getStatus)
                .containsOnly(StatusOS.EM_DIAGNOSTICO);
    }

    @Test
    @DisplayName("deve excluir FINALIZADA e ENTREGUE da listagem ativa")
    void deveExcluirFinalizadaEEntregueNaListagemAtiva() {
        OrdemDeServico osRecebida = criarOS("OS-20260604-EE0007", StatusOS.RECEBIDA);
        OrdemDeServico osEmExecucao = criarOS("OS-20260604-EE0008", StatusOS.EM_EXECUCAO);
        OrdemDeServico osFinalizada = criarOS("OS-20260604-EE0009", StatusOS.FINALIZADA);
        OrdemDeServico osEntregue = criarOS("OS-20260604-EE0010", StatusOS.ENTREGUE);

        ordemServicoRepository.save(osRecebida);
        ordemServicoRepository.save(osEmExecucao);
        ordemServicoRepository.save(osFinalizada);
        ordemServicoRepository.save(osEntregue);

        List<OrdemDeServico> ativas = ordemServicoRepository.findAllAtivasOrdered();

        assertThat(ativas).extracting(OrdemDeServico::getNumero)
                .contains("OS-20260604-EE0007", "OS-20260604-EE0008")
                .doesNotContain("OS-20260604-EE0009", "OS-20260604-EE0010");
        assertThat(ativas).extracting(OrdemDeServico::getStatus)
                .doesNotContain(StatusOS.FINALIZADA, StatusOS.ENTREGUE);
    }

    @Test
    @DisplayName("deve ordenar OS por prioridade de status")
    void deveOrdenarOSPorPrioridadeDeStatus() {
        OrdemDeServico osRecebida = criarOS("OS-20260604-FF0011", StatusOS.RECEBIDA);
        OrdemDeServico osEmExecucao = criarOS("OS-20260604-FF0012", StatusOS.EM_EXECUCAO);

        ordemServicoRepository.save(osRecebida);
        ordemServicoRepository.save(osEmExecucao);

        List<OrdemDeServico> ativas = ordemServicoRepository.findAllAtivasOrdered();

        List<OrdemDeServico> filtradas = ativas.stream()
                .filter(o -> o.getNumero().startsWith("OS-20260604-FF"))
                .toList();

        assertThat(filtradas).hasSizeGreaterThanOrEqualTo(2);
        assertThat(filtradas.get(0).getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    @DisplayName("deve verificar existência de OS ativa por veículo")
    void deveVerificarExistenciaDeOSAtivaPorVeiculo() {
        OrdemDeServico os = criarOS("OS-20260604-GG0013", StatusOS.RECEBIDA);
        OrdemDeServico salva = ordemServicoRepository.save(os);

        UUID veiculoId = salva.getVeiculo().getId();

        boolean existe = ordemServicoRepository.existsByVeiculoIdAndStatusNotIn(
                veiculoId,
                List.of(StatusOS.ENTREGUE, StatusOS.CANCELADA)
        );

        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("deve atualizar status da OS após avancarStatus()")
    void deveAtualizarStatusDaOS() {
        OrdemDeServico os = criarOS("OS-20260604-HH0014", StatusOS.RECEBIDA);
        OrdemDeServico salva = ordemServicoRepository.save(os);

        Optional<OrdemDeServico> carregada = ordemServicoRepository.findById(salva.getId());
        assertThat(carregada).isPresent();

        OrdemDeServico paraAtualizar = carregada.get();
        paraAtualizar.avancarStatus();
        ordemServicoRepository.save(paraAtualizar);

        Optional<OrdemDeServico> atualizada = ordemServicoRepository.findById(salva.getId());
        assertThat(atualizada).isPresent();
        assertThat(atualizada.get().getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
    }
}
