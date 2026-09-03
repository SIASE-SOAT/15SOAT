package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.input.ItemPecaRequest;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.model.Peca;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdicionarPecaUC: adicao de peca a OS")
class AdicionarPecaUCTest {

    @Mock private OrdemServicoRepositoryPort ordemServicoRepository;
    @Mock private PecaRepositoryPort pecaRepository;

    private AdicionarPecaUC useCase;
    private UUID osId;
    private UUID pecaId;
    private OrdemDeServico os;
    private Peca peca;

    @BeforeEach
    void setUp() {
        useCase = new AdicionarPecaUC(ordemServicoRepository, pecaRepository);
        osId = UUID.randomUUID();
        pecaId = UUID.randomUUID();

        Cliente cliente = new Cliente();
        cliente.setId(UUID.randomUUID());
        cliente.setNome("Carlos Silva");
        cliente.setEmail("carlos@email.com");

        Veiculo veiculo = new Veiculo();
        veiculo.setId(UUID.randomUUID());
        veiculo.setPlaca("MNO1234");
        veiculo.setModelo("HB20");

        os = OrdemDeServico.builder()
                .id(osId)
                .numero("OS-20260601-EEE555")
                .cliente(cliente)
                .veiculo(veiculo)
                .status(StatusOS.RECEBIDA)
                .build();

        peca = new Peca();
        peca.setId(pecaId);
        peca.setCodigo("OLEO-01");
        peca.setNome("Oleo 5W30");
        peca.setPreco(new BigDecimal("35.90"));
        peca.setQuantidadeEstoque(10);
        peca.setEstoqueMinimo(2);
        peca.setUnidadeMedida("L");
        peca.setAtivo(true);
    }

    @Test
    @DisplayName("Deve adicionar peca a OS e recalcular totais")
    void deveAdicionarPecaComSucesso() {
        ItemPecaRequest request = new ItemPecaRequest(pecaId, 2);
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        var response = useCase.executar(osId, request);

        assertThat(response).isNotNull();
        assertThat(os.getItensPeca()).hasSize(1);
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(8);
        verify(pecaRepository).save(argThat(p -> p.getQuantidadeEstoque() == 8));
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando OS nao encontrada")
    void deveLancarErroOsNaoEncontrada() {
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(osId, new ItemPecaRequest(pecaId, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando peca nao encontrada")
    void deveLancarErroPecaNaoEncontrada() {
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(osId, new ItemPecaRequest(pecaId, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lancar BusinessException quando peca esta desativada")
    void deveLancarErroPecaDesativada() {
        peca.setAtivo(false);
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));

        assertThatThrownBy(() -> useCase.executar(osId, new ItemPecaRequest(pecaId, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativada");
    }

    @Test
    @DisplayName("Deve lancar BusinessException quando estoque insuficiente")
    void deveLancarErroEstoqueInsuficiente() {
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));

        assertThatThrownBy(() -> useCase.executar(osId, new ItemPecaRequest(pecaId, 999)))
                .isInstanceOf(BusinessException.class);
    }
}
