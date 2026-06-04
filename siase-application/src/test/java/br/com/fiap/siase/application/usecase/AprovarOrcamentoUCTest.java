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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AprovarOrcamentoUC: aprovacao e recusa de orcamento")
class AprovarOrcamentoUCTest {

    @Mock private OrdemServicoRepositoryPort ordemServicoRepository;
    @Mock private EmailPort emailPort;

    private AprovarOrcamentoUC useCase;
    private OrdemDeServico os;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        useCase = new AprovarOrcamentoUC(ordemServicoRepository, emailPort);

        cliente = new Cliente();
        cliente.setNome("Maria");
        cliente.setEmail("maria@email.com");

        var veiculo = new Veiculo();
        veiculo.setPlaca("ABC1234");
        veiculo.setModelo("Civic");

        os = new OrdemDeServico();
        os.setNumero("OS-20260530-ABC123");
        os.setCliente(cliente);
        os.setVeiculo(veiculo);
    }

    @Test
    @DisplayName("Deve aprovar orcamento com sucesso")
    void deveAprovarOrcamentoComSucesso() {
        os.setStatus(StatusOS.AGUARDANDO_APROVACAO);

        when(ordemServicoRepository.findByNumero("OS-20260530-ABC123")).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        var response = useCase.aprovar("OS-20260530-ABC123");

        assertThat(response.status()).isEqualTo(StatusOS.APROVADO.name());
        verify(emailPort).enviarOrcamentoAprovado("maria@email.com", "Maria", "OS-20260530-ABC123");
    }

    @Test
    @DisplayName("Deve lancar erro se OS nao encontrada")
    void deveLancarErroOsNaoEncontrada() {
        when(ordemServicoRepository.findByNumero("OS-20260530-XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.aprovar("OS-20260530-XYZ"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lancar erro se OS nao esta aguardando aprovacao")
    void deveLancarErroStatusIncorreto() {
        os.setStatus(StatusOS.RECEBIDA);
        when(ordemServicoRepository.findByNumero("OS-20260530-ABC123")).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> useCase.aprovar("OS-20260530-ABC123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("aguardando");
    }

    @Test
    @DisplayName("Deve recusar orcamento e cancelar OS")
    void deveRecusarOrcamentoECancelar() {
        os.setStatus(StatusOS.AGUARDANDO_APROVACAO);

        when(ordemServicoRepository.findByNumero("OS-20260530-ABC123")).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        var response = useCase.recusar("OS-20260530-ABC123");

        assertThat(response.status()).isEqualTo(StatusOS.CANCELADA.name());
        verify(emailPort).enviarOrcamentoCancelado("maria@email.com", "Maria", "OS-20260530-ABC123");
    }
}
