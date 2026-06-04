package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.domain.enums.StatusOS;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListarOrdensServicoUC: listagem de ordens de servico")
class ListarOrdensServicoUCTest {

    @Mock
    private OrdemServicoRepositoryPort ordemServicoRepository;

    private ListarOrdensServicoUC useCase;
    private OrdemDeServico osAtiva;

    @BeforeEach
    void setUp() {
        useCase = new ListarOrdensServicoUC(ordemServicoRepository);

        Cliente cliente = new Cliente();
        cliente.setId(UUID.randomUUID());
        cliente.setNome("Joao Silva");
        cliente.setEmail("joao@email.com");

        Veiculo veiculo = new Veiculo();
        veiculo.setId(UUID.randomUUID());
        veiculo.setPlaca("ABC1234");
        veiculo.setModelo("Civic");

        osAtiva = OrdemDeServico.builder()
                .id(UUID.randomUUID())
                .numero("OS-20260601-AAA111")
                .cliente(cliente)
                .veiculo(veiculo)
                .status(StatusOS.EM_EXECUCAO)
                .build();
    }

    @Test
    @DisplayName("Sem filtro deve usar findAll retornando todas as OS")
    void semFiltroDeveUsarFindAll() {
        when(ordemServicoRepository.findAll()).thenReturn(List.of(osAtiva));

        List<OrdemDeServicoResponse> resultado = useCase.executar(null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).status()).isEqualTo(StatusOS.EM_EXECUCAO.name());
        verify(ordemServicoRepository).findAll();
        verifyNoMoreInteractions(ordemServicoRepository);
    }

    @Test
    @DisplayName("Com filtro de status deve usar findByStatus")
    void comFiltroDeveUsarFindByStatus() {
        when(ordemServicoRepository.findByStatus(StatusOS.RECEBIDA)).thenReturn(List.of(osAtiva));

        List<OrdemDeServicoResponse> resultado = useCase.executar(StatusOS.RECEBIDA);

        assertThat(resultado).hasSize(1);
        verify(ordemServicoRepository).findByStatus(StatusOS.RECEBIDA);
        verifyNoMoreInteractions(ordemServicoRepository);
    }

    @Test
    @DisplayName("Sem filtro retorna lista vazia quando nao ha OS")
    void semFiltroRetornaListaVazia() {
        when(ordemServicoRepository.findAll()).thenReturn(List.of());

        List<OrdemDeServicoResponse> resultado = useCase.executar(null);

        assertThat(resultado).isEmpty();
        verify(ordemServicoRepository).findAll();
    }

    @Test
    @DisplayName("Com filtro FINALIZADA deve usar findByStatus permitindo consultar OS ja encerradas")
    void comFiltroFinalizadaDeveUsarFindByStatus() {
        OrdemDeServico osFinalizada = OrdemDeServico.builder()
                .id(UUID.randomUUID())
                .numero("OS-20260601-FIN999")
                .cliente(osAtiva.getCliente())
                .veiculo(osAtiva.getVeiculo())
                .status(StatusOS.FINALIZADA)
                .build();

        when(ordemServicoRepository.findByStatus(StatusOS.FINALIZADA)).thenReturn(List.of(osFinalizada));

        List<OrdemDeServicoResponse> resultado = useCase.executar(StatusOS.FINALIZADA);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).status()).isEqualTo(StatusOS.FINALIZADA.name());
        verify(ordemServicoRepository).findByStatus(StatusOS.FINALIZADA);
    }
}
