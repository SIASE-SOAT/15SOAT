package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelarOrdemUC: cancelamento de ordem de servico")
class CancelarOrdemUCTest {

    @Mock private OrdemServicoRepositoryPort ordemServicoRepository;

    private CancelarOrdemUC useCase;
    private UUID osId;
    private OrdemDeServico os;

    @BeforeEach
    void setUp() {
        useCase = new CancelarOrdemUC(ordemServicoRepository);
        osId = UUID.randomUUID();

        Cliente cliente = new Cliente();
        cliente.setId(UUID.randomUUID());
        cliente.setNome("Ana Lima");
        cliente.setEmail("ana@email.com");

        Veiculo veiculo = new Veiculo();
        veiculo.setId(UUID.randomUUID());
        veiculo.setPlaca("GHI3456");
        veiculo.setModelo("Palio");

        os = OrdemDeServico.builder()
                .id(osId)
                .numero("OS-20260601-CCC333")
                .cliente(cliente)
                .veiculo(veiculo)
                .status(StatusOS.RECEBIDA)
                .build();
    }

    @Test
    @DisplayName("Deve cancelar OS com sucesso")
    void deveCancelarOSComSucesso() {
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any())).thenReturn(os);

        var response = useCase.executar(osId);

        assertThat(response.status()).isEqualTo(StatusOS.CANCELADA.name());
        assertThat(os.getDataFechamento()).isNotNull();
        verify(ordemServicoRepository).save(os);
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
    @DisplayName("Deve lancar BusinessException quando OS em execucao nao pode ser cancelada")
    void deveLancarErroOsEmExecucao() {
        os.setStatus(StatusOS.EM_EXECUCAO);
        when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> useCase.executar(osId))
                .isInstanceOf(BusinessException.class);
    }
}
