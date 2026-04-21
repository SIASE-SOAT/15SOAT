package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.AgendamentoRequest;
import br.com.fiap.siase.dto.response.AgendamentoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Agendamento;
import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.Veiculo;
import br.com.fiap.siase.model.enums.StatusAgendamento;
import br.com.fiap.siase.repository.AgendamentoRepository;
import br.com.fiap.siase.repository.ClienteRepository;
import br.com.fiap.siase.repository.VeiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgendamentoService - Regras de Negócio")
class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository repository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AgendamentoService service;

    private UUID clienteId;
    private UUID veiculoId;
    private UUID agendamentoId;
    private Cliente cliente;
    private Veiculo veiculo;
    private Agendamento agendamento;
    private AgendamentoRequest request;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
        agendamentoId = UUID.randomUUID();

        cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", clienteId);
        cliente.setNome("Maria Oliveira");
        cliente.setEmail("maria@email.com");

        veiculo = new Veiculo();
        ReflectionTestUtils.setField(veiculo, "id", veiculoId);
        veiculo.setCliente(cliente);
        veiculo.setMarca("Toyota");
        veiculo.setModelo("Corolla");
        veiculo.setPlaca("ABC1234");

        agendamento = new Agendamento();
        ReflectionTestUtils.setField(agendamento, "id", agendamentoId);
        agendamento.setCliente(cliente);
        agendamento.setVeiculo(veiculo);
        agendamento.setDataHora(LocalDateTime.now().plusDays(1));
        agendamento.setDescricaoServicos("Revisão e alinhamento");

        request = new AgendamentoRequest(
                clienteId,
                veiculoId,
                LocalDateTime.now().plusDays(2),
                "Revisão e alinhamento"
        );
    }

    @Nested
    @DisplayName("Criar")
    class Criar {

        @Test
        @DisplayName("Deve criar agendamento e enviar confirmação")
        void deveCriarAgendamentoComSucesso() {
            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(repository.save(any(Agendamento.class))).thenReturn(agendamento);

            AgendamentoResponse response = service.criar(request);

            assertThat(response.clienteId()).isEqualTo(clienteId);
            assertThat(response.veiculoId()).isEqualTo(veiculoId);
            verify(emailService).enviarConfirmacaoAgendamento(any(), eq("Maria Oliveira"), any(), contains("ABC1234"));
        }

        @Test
        @DisplayName("Deve rejeitar veículo de outro cliente")
        void deveRejeitarVeiculoDeOutroCliente() {
            Cliente outroCliente = new Cliente();
            ReflectionTestUtils.setField(outroCliente, "id", UUID.randomUUID());
            veiculo.setCliente(outroCliente);

            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));

            assertThatThrownBy(() -> service.criar(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não pertence ao cliente");
        }
    }

    @Nested
    @DisplayName("Consultas e transições")
    class ConsultasETransicoes {

        @Test
        @DisplayName("Deve listar todos os agendamentos")
        void deveListarTodos() {
            when(repository.findAll()).thenReturn(List.of(agendamento));

            assertThat(service.listar()).hasSize(1);
        }

        @Test
        @DisplayName("Deve listar por status")
        void deveListarPorStatus() {
            when(repository.findByStatus(StatusAgendamento.AGENDADO)).thenReturn(List.of(agendamento));

            assertThat(service.listarPorStatus(StatusAgendamento.AGENDADO)).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar por cliente")
        void deveListarPorCliente() {
            when(repository.findByClienteId(clienteId)).thenReturn(List.of(agendamento));

            assertThat(service.listarPorCliente(clienteId)).hasSize(1);
        }

        @Test
        @DisplayName("Deve confirmar agendamento existente")
        void deveConfirmarAgendamento() {
            when(repository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
            when(repository.save(any(Agendamento.class))).thenReturn(agendamento);

            AgendamentoResponse response = service.confirmar(agendamentoId);

            assertThat(response.status()).isEqualTo("CONFIRMADO");
        }

        @Test
        @DisplayName("Deve cancelar agendamento existente")
        void deveCancelarAgendamento() {
            when(repository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
            when(repository.save(any(Agendamento.class))).thenReturn(agendamento);

            AgendamentoResponse response = service.cancelar(agendamentoId);

            assertThat(response.status()).isEqualTo("CANCELADO");
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException ao buscar id inexistente")
        void deveLancarQuandoIdNaoExiste() {
            when(repository.findById(agendamentoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(agendamentoId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Agendamento não encontrado");
        }
    }
}
