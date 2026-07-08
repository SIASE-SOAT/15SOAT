package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.EmailPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvancarStatusUC: avanco de status da ordem de servico")
class AvancarStatusUCTest {

    @Mock private OrdemServicoRepositoryPort ordemServicoRepository;
    @Mock private EmailPort emailPort;

    private AvancarStatusUC useCase;
    private UUID osId;
    private OrdemDeServico os;

    @BeforeEach
    void setUp() {
        useCase = new AvancarStatusUC(ordemServicoRepository, emailPort);
        osId = UUID.randomUUID();

        Cliente cliente = new Cliente();
        cliente.setId(UUID.randomUUID());
        cliente.setNome("Carlos Andrade");
        cliente.setEmail("carlos@email.com");

        Veiculo veiculo = new Veiculo();
        veiculo.setId(UUID.randomUUID());
        veiculo.setPlaca("DEF5678");
        veiculo.setModelo("Onix");

        os = OrdemDeServico.builder()
                .id(osId)
                .numero("OS-20260601-DEF456")
                .cliente(cliente)
                .veiculo(veiculo)
                .status(StatusOS.RECEBIDA)
                .build();
    }

    @Test
    @DisplayName("Deve avancar status de RECEBIDA para EM_DIAGNOSTICO com sucesso")
    void deveAvancarStatusComSucesso() {
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        var response = useCase.executar(osId);

        assertThat(response.status()).isEqualTo(StatusOS.EM_DIAGNOSTICO.name());
        verify(ordemServicoRepository).save(os);
        verify(emailPort, never()).enviarOrcamentoParaAprovacao(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve disparar email de orcamento para aprovacao ao avancar para AGUARDANDO_APROVACAO")
    void deveDispararEmailAoAvancarParaAguardandoAprovacao() {
        os.setStatus(StatusOS.EM_DIAGNOSTICO);
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        useCase.executar(osId);

        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        verify(emailPort).enviarOrcamentoParaAprovacao(
                "carlos@email.com", "Carlos Andrade", "OS-20260601-DEF456", os.getTotal().toPlainString());
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando OS nao encontrada")
    void deveLancarErroOsNaoEncontrada() {
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(osId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(osId.toString());
    }

    @Test
    @DisplayName("Deve lancar BusinessException quando OS ja esta entregue")
    void deveLancarErroOsJaEntregue() {
        os.setStatus(StatusOS.ENTREGUE);
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> useCase.executar(osId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entregue");
    }

    @Test
    @DisplayName("Deve lancar BusinessException quando OS esta cancelada")
    void deveLancarErroOsCancelada() {
        os.setStatus(StatusOS.CANCELADA);
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> useCase.executar(osId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelad");
    }

    @Test
    @DisplayName("Deve registrar data de fechamento ao avancar para ENTREGUE")
    void deveRegistrarDataFechamentoAoAvancarParaEntregue() {
        os.setStatus(StatusOS.FINALIZADA);
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        useCase.executar(osId);

        assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);
        assertThat(os.getDataFechamento()).isNotNull();
    }
}
