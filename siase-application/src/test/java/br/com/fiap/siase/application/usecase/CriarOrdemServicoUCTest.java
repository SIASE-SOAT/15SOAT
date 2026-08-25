package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.input.ItemServicoRequest;
import br.com.fiap.siase.application.dto.input.OrdemDeServicoRequest;
import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.port.ObservabilityPort;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.Servico;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CriarOrdemServicoUC: abertura de OS")
class CriarOrdemServicoUCTest {

    @Mock private OrdemServicoRepositoryPort ordemServicoRepository;
    @Mock private ClienteRepositoryPort clienteRepository;
    @Mock private VeiculoRepositoryPort veiculoRepository;
    @Mock private ServicoRepositoryPort servicoRepository;
    @Mock private PecaRepositoryPort pecaRepository;
    @Mock private ObservabilityPort observability;

    private CriarOrdemServicoUC useCase;
    private UUID clienteId;
    private UUID veiculoId;
    private UUID servicoId;
    private Cliente cliente;
    private Veiculo veiculo;
    private Servico servico;

    @BeforeEach
    void setUp() {
        useCase = new CriarOrdemServicoUC(ordemServicoRepository, clienteRepository,
                veiculoRepository, servicoRepository, pecaRepository, observability);

        clienteId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
        servicoId = UUID.randomUUID();

        cliente = new Cliente();
        cliente.setId(clienteId);
        cliente.setNome("Joao");
        cliente.setDocumento("12345678901");

        veiculo = new Veiculo();
        veiculo.setId(veiculoId);
        veiculo.setCliente(cliente);
        veiculo.setPlaca("ABC1234");
        veiculo.setMarca("Honda");
        veiculo.setModelo("Civic");
        veiculo.setAno(2020);

        servico = new Servico();
        servico.setId(servicoId);
        servico.setNome("Troca de Oleo");
        servico.setPreco(new BigDecimal("150.00"));
        servico.setAtivo(true);
    }

    @Test
    @DisplayName("Deve criar OS com sucesso (fluxo feliz)")
    void deveCriarOsComSucesso() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(ordemServicoRepository.existsByVeiculoIdAndStatusNotIn(any(), any())).thenReturn(false);
        when(ordemServicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new OrdemDeServicoRequest(clienteId, veiculoId, null,
                List.of(new ItemServicoRequest(servicoId, null)), null);

        var response = useCase.executar(request);

        assertThat(response).isNotNull();
        assertThat(response.numero()).startsWith("OS-");
        assertThat(response.clienteId()).isEqualTo(clienteId);
        assertThat(response.status()).isEqualTo(StatusOS.RECEBIDA.name());
    }

    @Test
    @DisplayName("Deve lancar erro se cliente nao encontrado")
    void deveLancarErroClienteNaoEncontrado() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        var request = new OrdemDeServicoRequest(clienteId, veiculoId, null,
                List.of(new ItemServicoRequest(servicoId, null)), null);

        assertThatThrownBy(() -> useCase.executar(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente n");
    }

    @Test
    @DisplayName("Deve lancar erro se veiculo nao pertence ao cliente")
    void deveLancarErroVeiculoNaoPertence() {
        var outroCliente = new Cliente();
        outroCliente.setId(UUID.randomUUID());
        veiculo.setCliente(outroCliente);

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));

        var request = new OrdemDeServicoRequest(clienteId, veiculoId, null,
                List.of(new ItemServicoRequest(servicoId, null)), null);

        assertThatThrownBy(() -> useCase.executar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pertence ao cliente");
    }

    @Test
    @DisplayName("Deve lancar erro se veiculo ja tem OS em andamento")
    void deveLancarErroVeiculoComOsEmAndamento() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(ordemServicoRepository.existsByVeiculoIdAndStatusNotIn(any(), any())).thenReturn(true);

        var request = new OrdemDeServicoRequest(clienteId, veiculoId, null,
                List.of(new ItemServicoRequest(servicoId, null)), null);

        assertThatThrownBy(() -> useCase.executar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ordem de servi");
    }
}
