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
@DisplayName("AtualizarStatusViaWebhookUC: atualizacao de status via webhook")
class AtualizarStatusViaWebhookUCTest {

    private static final String TOKEN_VALIDO = "token-secreto-123";
    private static final String NUMERO_OS = "OS-20260530-ABC123";

    @Mock private OrdemServicoRepositoryPort ordemServicoRepository;
    @Mock private EmailPort emailPort;

    private AtualizarStatusViaWebhookUC useCase;
    private OrdemDeServico os;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        useCase = new AtualizarStatusViaWebhookUC(ordemServicoRepository, emailPort, TOKEN_VALIDO);

        cliente = new Cliente();
        cliente.setNome("Joao Silva");
        cliente.setEmail("joao@email.com");

        Veiculo veiculo = new Veiculo();
        veiculo.setId(UUID.randomUUID());
        veiculo.setPlaca("XYZ9876");
        veiculo.setModelo("Gol");

        os = OrdemDeServico.builder()
                .id(UUID.randomUUID())
                .numero(NUMERO_OS)
                .cliente(cliente)
                .veiculo(veiculo)
                .status(StatusOS.RECEBIDA)
                .build();
    }

    @Test
    @DisplayName("Deve rejeitar token invalido com BusinessException sem consultar repositorio")
    void deveRejeitarTokenInvalido() {
        assertThatThrownBy(() -> useCase.executar(NUMERO_OS, "EM_DIAGNOSTICO", "token-errado"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Token");

        verify(ordemServicoRepository, never()).findByNumero(any());
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando OS nao encontrada")
    void deveLancarErroOsNaoEncontrada() {
        when(ordemServicoRepository.findByNumero(NUMERO_OS)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(NUMERO_OS, "EM_DIAGNOSTICO", TOKEN_VALIDO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(NUMERO_OS);
    }

    @Test
    @DisplayName("Deve lancar BusinessException para status invalido")
    void deveLancarErroParaStatusInvalido() {
        when(ordemServicoRepository.findByNumero(NUMERO_OS)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> useCase.executar(NUMERO_OS, "STATUS_INEXISTENTE", TOKEN_VALIDO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Status inv");
    }

    @Test
    @DisplayName("Deve cancelar OS com sucesso quando novoStatus e CANCELADA")
    void deveCancelarOsComSucesso() {
        os.setStatus(StatusOS.AGUARDANDO_APROVACAO);
        when(ordemServicoRepository.findByNumero(NUMERO_OS)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        var response = useCase.executar(NUMERO_OS, "CANCELADA", TOKEN_VALIDO);

        assertThat(response.status()).isEqualTo(StatusOS.CANCELADA.name());
        assertThat(os.getDataFechamento()).isNotNull();
        verify(ordemServicoRepository).save(os);
    }

    @Test
    @DisplayName("Deve avancar status de RECEBIDA para EM_DIAGNOSTICO com sucesso")
    void deveAvancarStatusComSucesso() {
        os.setStatus(StatusOS.RECEBIDA);
        when(ordemServicoRepository.findByNumero(NUMERO_OS)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        var response = useCase.executar(NUMERO_OS, "EM_DIAGNOSTICO", TOKEN_VALIDO);

        assertThat(response.status()).isEqualTo(StatusOS.EM_DIAGNOSTICO.name());
        verify(ordemServicoRepository).save(os);
    }

    @Test
    @DisplayName("Deve lancar BusinessException para transicao de status invalida")
    void deveLancarErroParaTransicaoInvalida() {
        os.setStatus(StatusOS.RECEBIDA);
        when(ordemServicoRepository.findByNumero(NUMERO_OS)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> useCase.executar(NUMERO_OS, "FINALIZADA", TOKEN_VALIDO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transi");
    }

    @Test
    @DisplayName("Deve enviar email de orcamento aprovado quando status avancar para APROVADO")
    void deveEnviarEmailAprovadoQuandoStatusAprovado() {
        os.setStatus(StatusOS.AGUARDANDO_APROVACAO);
        when(ordemServicoRepository.findByNumero(NUMERO_OS)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        useCase.executar(NUMERO_OS, "APROVADO", TOKEN_VALIDO);

        verify(emailPort).enviarOrcamentoAprovado("joao@email.com", "Joao Silva", NUMERO_OS);
    }

    @Test
    @DisplayName("Deve enviar email de cancelamento quando OS for cancelada")
    void deveEnviarEmailCancelamentoQuandoOsCancelada() {
        os.setStatus(StatusOS.AGUARDANDO_APROVACAO);
        when(ordemServicoRepository.findByNumero(NUMERO_OS)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        useCase.executar(NUMERO_OS, "CANCELADA", TOKEN_VALIDO);

        verify(emailPort).enviarOrcamentoCancelado("joao@email.com", "Joao Silva", NUMERO_OS);
    }
}
