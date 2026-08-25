package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.port.ObservabilityPort;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.ItemServico;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.model.Servico;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("IniciarExecucaoItemUC / FinalizarExecucaoItemUC: execucao de itens de servico")
class IniciarFinalizarExecucaoItemUCTest {

    @Mock private OrdemServicoRepositoryPort ordemServicoRepository;
    @Mock private ObservabilityPort observabilityPort;

    private IniciarExecucaoItemUC iniciarUC;
    private FinalizarExecucaoItemUC finalizarUC;
    private UUID osId;
    private UUID itemId;
    private OrdemDeServico os;
    private ItemServico item;

    @BeforeEach
    void setUp() {
        iniciarUC = new IniciarExecucaoItemUC(ordemServicoRepository,observabilityPort);
        finalizarUC = new FinalizarExecucaoItemUC(ordemServicoRepository,observabilityPort);
        osId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        Cliente cliente = new Cliente();
        cliente.setId(UUID.randomUUID());
        cliente.setNome("Lucas Mota");
        cliente.setEmail("lucas@email.com");

        Veiculo veiculo = new Veiculo();
        veiculo.setId(UUID.randomUUID());
        veiculo.setPlaca("STU9012");
        veiculo.setModelo("Fusca");

        Servico servico = new Servico();
        servico.setId(UUID.randomUUID());
        servico.setNome("Balanceamento");
        servico.setPreco(new BigDecimal("80.00"));

        item = new ItemServico();
        item.setId(itemId);
        item.setServico(servico);
        item.setPrecoUnitario(servico.getPreco());

        os = OrdemDeServico.builder()
                .id(osId)
                .numero("OS-20260601-GGG777")
                .cliente(cliente)
                .veiculo(veiculo)
                .status(StatusOS.EM_EXECUCAO)
                .build();
        os.getItensServico().add(item);
    }

    @Nested
    @DisplayName("IniciarExecucaoItemUC")
    class IniciarExecucao {

        @Test
        @DisplayName("Deve iniciar execucao de item com sucesso")
        void deveIniciarExecucaoComSucesso() {
            when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
            when(ordemServicoRepository.save(any())).thenReturn(os);

            var response = iniciarUC.executar(osId, itemId);

            assertThat(response).isNotNull();
            assertThat(item.getDataInicioExecucao()).isNotNull();
        }

        @Test
        @DisplayName("Deve lancar BusinessException quando OS nao esta em execucao")
        void deveLancarErroOsNaoEmExecucao() {
            os.setStatus(StatusOS.RECEBIDA);
            when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));

            assertThatThrownBy(() -> iniciarUC.executar(osId, itemId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("execu");
        }

        @Test
        @DisplayName("Deve lancar ResourceNotFoundException quando item nao encontrado")
        void deveLancarErroItemNaoEncontrado() {
            when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));

            assertThatThrownBy(() -> iniciarUC.executar(osId, UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve lancar ResourceNotFoundException quando OS nao encontrada")
        void deveLancarErroOsNaoEncontrada() {
            when(ordemServicoRepository.findById(osId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> iniciarUC.executar(osId, itemId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("FinalizarExecucaoItemUC")
    class FinalizarExecucao {

        @Test
        @DisplayName("Deve finalizar execucao de item com sucesso")
        void deveFinalizarExecucaoComSucesso() {
            when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));
            when(ordemServicoRepository.save(any())).thenReturn(os);

            var response = finalizarUC.executar(osId, itemId);

            assertThat(response).isNotNull();
            assertThat(item.getDataFimExecucao()).isNotNull();
        }

        @Test
        @DisplayName("Deve lancar BusinessException quando OS nao esta em execucao")
        void deveLancarErroOsNaoEmExecucao() {
            os.setStatus(StatusOS.RECEBIDA);
            when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(os));

            assertThatThrownBy(() -> finalizarUC.executar(osId, itemId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Deve lancar ResourceNotFoundException quando OS nao encontrada")
        void deveLancarErroOsNaoEncontrada() {
            when(ordemServicoRepository.findById(osId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> finalizarUC.executar(osId, itemId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
