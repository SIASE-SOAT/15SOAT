package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.model.Veiculo;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarStatusOSUC: consulta de status de OS")
class ConsultarStatusOSUCTest {

    @Mock private OrdemServicoRepositoryPort ordemServicoRepository;

    private ConsultarStatusOSUC useCase;
    private OrdemDeServico os;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarStatusOSUC(ordemServicoRepository);

        Cliente cliente = new Cliente();
        cliente.setId(UUID.randomUUID());
        cliente.setNome("Pedro Santos");
        cliente.setEmail("pedro@email.com");

        Veiculo veiculo = new Veiculo();
        veiculo.setId(UUID.randomUUID());
        veiculo.setPlaca("JKL7890");
        veiculo.setModelo("Uno");

        os = OrdemDeServico.builder()
                .id(UUID.randomUUID())
                .numero("OS-20260601-DDD444")
                .cliente(cliente)
                .veiculo(veiculo)
                .status(StatusOS.EM_DIAGNOSTICO)
                .build();
    }

    @Test
    @DisplayName("Deve retornar OS por ID")
    void deveRetornarOSPorId() {
        when(ordemServicoRepository.findById(os.getId())).thenReturn(Optional.of(os));

        var response = useCase.executar(os.getId());

        assertThat(response.id()).isEqualTo(os.getId());
        assertThat(response.status()).isEqualTo(StatusOS.EM_DIAGNOSTICO.name());
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException ao buscar por ID inexistente")
    void deveLancarErroIdNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        when(ordemServicoRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve retornar OS por numero")
    void deveRetornarOSPorNumero() {
        when(ordemServicoRepository.findByNumero("OS-20260601-DDD444")).thenReturn(Optional.of(os));

        var response = useCase.executarPorNumero("OS-20260601-DDD444");

        assertThat(response.numero()).isEqualTo("OS-20260601-DDD444");
        assertThat(response.status()).isEqualTo(StatusOS.EM_DIAGNOSTICO.name());
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException ao buscar por numero inexistente")
    void deveLancarErroNumeroNaoEncontrado() {
        when(ordemServicoRepository.findByNumero("OS-INVALIDO")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executarPorNumero("OS-INVALIDO"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
