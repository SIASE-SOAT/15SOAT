package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.input.ItemServicoRequest;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.ItemServico;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.model.Servico;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdicionarServicoUC: adicao de servico a OS")
class AdicionarServicoUCTest {

    @Mock private OrdemServicoRepositoryPort ordemServicoRepository;
    @Mock private ServicoRepositoryPort servicoRepository;

    private AdicionarServicoUC useCase;
    private UUID osId;
    private UUID servicoId;
    private OrdemDeServico os;
    private Servico servico;

    @BeforeEach
    void setUp() {
        useCase = new AdicionarServicoUC(ordemServicoRepository, servicoRepository);
        osId = UUID.randomUUID();
        servicoId = UUID.randomUUID();

        Cliente cliente = new Cliente();
        cliente.setId(UUID.randomUUID());
        cliente.setNome("Maria Costa");
        cliente.setEmail("maria@email.com");

        Veiculo veiculo = new Veiculo();
        veiculo.setId(UUID.randomUUID());
        veiculo.setPlaca("PQR5678");
        veiculo.setModelo("Celta");

        os = OrdemDeServico.builder()
                .id(osId)
                .numero("OS-20260601-FFF666")
                .cliente(cliente)
                .veiculo(veiculo)
                .status(StatusOS.RECEBIDA)
                .build();

        servico = new Servico();
        servico.setId(servicoId);
        servico.setNome("Alinhamento");
        servico.setPreco(new BigDecimal("120.00"));
        servico.setTempoEstimadoMinutos(45);
        servico.setAtivo(true);
    }

    @Test
    @DisplayName("Deve adicionar servico a OS e recalcular totais")
    void deveAdicionarServicoComSucesso() {
        ItemServicoRequest request = new ItemServicoRequest(servicoId, "Alinhamento das 4 rodas");
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        var response = useCase.executar(osId, request);

        assertThat(response).isNotNull();
        assertThat(os.getItensServico()).hasSize(1);
        assertThat(os.getTotalServicos()).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando OS nao encontrada")
    void deveLancarErroOsNaoEncontrada() {
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(osId, new ItemServicoRequest(servicoId, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando servico nao encontrado")
    void deveLancarErroServicoNaoEncontrado() {
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(osId, new ItemServicoRequest(servicoId, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lancar BusinessException quando servico esta desativado")
    void deveLancarErroServicoDesativado() {
        servico.setAtivo(false);
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));

        assertThatThrownBy(() -> useCase.executar(osId, new ItemServicoRequest(servicoId, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativado");
    }

    @Test
    @DisplayName("Deve lancar BusinessException quando servico ja adicionado")
    void deveLancarErroServicoDuplicado() {
        ItemServico itemExistente = new ItemServico();
        itemExistente.setServico(servico);
        os.getItensServico().add(itemExistente);

        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));

        assertThatThrownBy(() -> useCase.executar(osId, new ItemServicoRequest(servicoId, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já foi adicionado");
    }
}
