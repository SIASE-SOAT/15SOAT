package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.domain.enums.TipoPessoa;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrepararAberturaOSUC: preparacao para abertura de OS")
class PrepararAberturaOSUCTest {

    @Mock private ClienteRepositoryPort clienteRepository;
    @Mock private VeiculoRepositoryPort veiculoRepository;

    private PrepararAberturaOSUC useCase;
    private Cliente cliente;
    private Veiculo veiculo;

    @BeforeEach
    void setUp() {
        useCase = new PrepararAberturaOSUC(clienteRepository, veiculoRepository);

        cliente = new Cliente();
        cliente.setId(UUID.randomUUID());
        cliente.setNome("Roberto Alves");
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setDocumento("52998224725");
        cliente.setEmail("roberto@email.com");
        cliente.setAtivo(true);

        veiculo = new Veiculo();
        veiculo.setId(UUID.randomUUID());
        veiculo.setPlaca("VWX3456");
        veiculo.setMarca("VW");
        veiculo.setModelo("Gol");
        veiculo.setAno(2019);
        veiculo.setAtivo(true);
        veiculo.setCliente(cliente);
    }

    @Test
    @DisplayName("Deve preparar abertura sem placa retornando veiculos ativos do cliente")
    void devePrepararAberturasSemPlaca() {
        when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findByClienteId(cliente.getId())).thenReturn(List.of(veiculo));

        var response = useCase.executar("529.982.247-25", null);

        assertThat(response.cliente().nome()).isEqualTo("Roberto Alves");
        assertThat(response.veiculos()).hasSize(1);
        assertThat(response.veiculoSelecionado()).isNull();
        assertThat(response.prontoParaAbertura()).isFalse();
    }

    @Test
    @DisplayName("Deve preparar abertura com placa retornando veiculo selecionado")
    void devePrepararAberturaComPlaca() {
        when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findByClienteId(cliente.getId())).thenReturn(List.of(veiculo));
        when(veiculoRepository.findByPlaca("VWX3456")).thenReturn(Optional.of(veiculo));

        var response = useCase.executar("529.982.247-25", "VWX3456");

        assertThat(response.veiculoSelecionado()).isNotNull();
        assertThat(response.veiculoSelecionado().placa()).isEqualTo("VWX3456");
        assertThat(response.prontoParaAbertura()).isTrue();
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando cliente nao encontrado")
    void deveLancarErroClienteNaoEncontrado() {
        when(clienteRepository.findByDocumento("00000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar("000.000.000-00", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente não encontrado");
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando veiculo nao encontrado pela placa")
    void deveLancarErroVeiculoNaoEncontrado() {
        when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findByClienteId(cliente.getId())).thenReturn(List.of(veiculo));
        when(veiculoRepository.findByPlaca("ZZZ9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar("529.982.247-25", "ZZZ9999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lancar BusinessException quando veiculo nao pertence ao cliente")
    void deveLancarErroVeiculoNaoPertenceAoCliente() {
        Cliente outroCliente = new Cliente();
        outroCliente.setId(UUID.randomUUID());
        veiculo.setCliente(outroCliente);

        when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findByClienteId(cliente.getId())).thenReturn(List.of());
        when(veiculoRepository.findByPlaca("VWX3456")).thenReturn(Optional.of(veiculo));

        assertThatThrownBy(() -> useCase.executar("529.982.247-25", "VWX3456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pertence");
    }

    @Test
    @DisplayName("Deve lancar BusinessException quando veiculo esta inativo")
    void deveLancarErroVeiculoInativo() {
        veiculo.setAtivo(false);

        when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findByClienteId(cliente.getId())).thenReturn(List.of());
        when(veiculoRepository.findByPlaca("VWX3456")).thenReturn(Optional.of(veiculo));

        assertThatThrownBy(() -> useCase.executar("529.982.247-25", "VWX3456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inativo");
    }
}
