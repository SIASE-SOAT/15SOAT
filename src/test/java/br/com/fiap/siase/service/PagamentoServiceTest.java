package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.PagamentoRequest;
import br.com.fiap.siase.dto.response.PagamentoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.OrdemDeServico;
import br.com.fiap.siase.model.Pagamento;
import br.com.fiap.siase.model.enums.FormaPagamento;
import br.com.fiap.siase.model.enums.StatusOS;
import br.com.fiap.siase.model.enums.StatusPagamento;
import br.com.fiap.siase.model.enums.TipoPessoa;
import br.com.fiap.siase.repository.OrdemDeServicoRepository;
import br.com.fiap.siase.repository.PagamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagamentoService - Regras de Negócio")
class PagamentoServiceTest {

    @Mock
    private PagamentoRepository repository;

    @Mock
    private OrdemDeServicoRepository osRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PagamentoService pagamentoService;

    private OrdemDeServico os;
    private Pagamento pagamento;
    private UUID osId;
    private UUID pagamentoId;

    @BeforeEach
    void setUp() {
        osId = UUID.randomUUID();
        pagamentoId = UUID.randomUUID();

        Cliente cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", UUID.randomUUID());
        cliente.setNome("João Silva");
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setDocumento("52998224725");
        cliente.setEmail("joao@email.com");

        os = new OrdemDeServico();
        ReflectionTestUtils.setField(os, "id", osId);
        ReflectionTestUtils.setField(os, "criadoEm", LocalDateTime.now());
        ReflectionTestUtils.setField(os, "atualizadoEm", LocalDateTime.now());
        os.setNumero("OS-001");
        os.setStatus(StatusOS.FINALIZADA);
        os.setCliente(cliente);
        os.setTotal(new BigDecimal("500.00"));

        pagamento = new Pagamento();
        ReflectionTestUtils.setField(pagamento, "id", pagamentoId);
        ReflectionTestUtils.setField(pagamento, "criadoEm", LocalDateTime.now());
        ReflectionTestUtils.setField(pagamento, "atualizadoEm", LocalDateTime.now());
        pagamento.setOrdemDeServico(os);
        pagamento.setFormaPagamento(FormaPagamento.PIX);
        pagamento.setValor(new BigDecimal("500.00"));
    }

    // -----------------------------------------------------------------------
    // REGISTRAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Registrar pagamento")
    class Registrar {

        @Test
        @DisplayName("Deve registrar pagamento para OS finalizada")
        void deveRegistrarComSucesso() {
            when(osRepository.findById(osId)).thenReturn(Optional.of(os));
            when(repository.existsByOrdemDeServicoId(osId)).thenReturn(false);
            when(repository.save(any(Pagamento.class))).thenReturn(pagamento);

            PagamentoResponse response = pagamentoService.registrar(osId,
                    new PagamentoRequest(FormaPagamento.PIX, new BigDecimal("500.00")));

            assertThat(response).isNotNull();
            assertThat(response.ordemDeServicoId()).isEqualTo(osId);
            assertThat(response.formaPagamento()).isEqualTo("PIX");
        }

        @Test
        @DisplayName("Deve lançar exceção quando OS não encontrada")
        void deveLancarExcecaoQuandoOsNaoEncontrada() {
            when(osRepository.findById(osId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagamentoService.registrar(osId,
                    new PagamentoRequest(FormaPagamento.PIX, new BigDecimal("500.00"))))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando OS não está FINALIZADA")
        void deveLancarExcecaoQuandoOsNaoFinalizada() {
            os.setStatus(StatusOS.EM_EXECUCAO);
            when(osRepository.findById(osId)).thenReturn(Optional.of(os));

            assertThatThrownBy(() -> pagamentoService.registrar(osId,
                    new PagamentoRequest(FormaPagamento.PIX, new BigDecimal("500.00"))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("FINALIZADA");
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando OS já tem pagamento")
        void deveLancarExcecaoQuandoOsJaTemPagamento() {
            when(osRepository.findById(osId)).thenReturn(Optional.of(os));
            when(repository.existsByOrdemDeServicoId(osId)).thenReturn(true);

            assertThatThrownBy(() -> pagamentoService.registrar(osId,
                    new PagamentoRequest(FormaPagamento.PIX, new BigDecimal("500.00"))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já possui um pagamento");
        }
    }

    // -----------------------------------------------------------------------
    // BUSCAR POR OS
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Buscar pagamento por OS")
    class BuscarPorOS {

        @Test
        @DisplayName("Deve retornar pagamento da OS")
        void deveRetornarPagamento() {
            when(repository.findByOrdemDeServicoId(osId)).thenReturn(Optional.of(pagamento));

            PagamentoResponse response = pagamentoService.buscarPorOS(osId);

            assertThat(response.ordemDeServicoId()).isEqualTo(osId);
        }

        @Test
        @DisplayName("Deve lançar exceção quando pagamento não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(repository.findByOrdemDeServicoId(osId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagamentoService.buscarPorOS(osId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // CONFIRMAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Confirmar pagamento")
    class Confirmar {

        @Test
        @DisplayName("Deve confirmar pagamento pendente e avançar OS para ENTREGUE")
        void deveConfirmarPagamento() {
            when(repository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
            when(repository.save(any(Pagamento.class))).thenReturn(pagamento);
            when(osRepository.save(any(OrdemDeServico.class))).thenReturn(os);
            doNothing().when(emailService).enviarConfirmacaoPagamento(any(), any(), any(), any(), any());

            PagamentoResponse response = pagamentoService.confirmar(pagamentoId);

            assertThat(response).isNotNull();
            assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PAGO);
            verify(osRepository).save(os);
        }

        @Test
        @DisplayName("Deve lançar BusinessException ao confirmar pagamento já pago")
        void deveLancarExcecaoAoConfirmarPago() {
            pagamento.confirmar(); // status -> PAGO
            when(repository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));

            assertThatThrownBy(() -> pagamentoService.confirmar(pagamentoId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("pendentes podem ser confirmados");
        }

        @Test
        @DisplayName("Deve lançar exceção quando pagamento não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagamentoService.confirmar(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // CANCELAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cancelar pagamento")
    class Cancelar {

        @Test
        @DisplayName("Deve cancelar pagamento pendente")
        void deveCancelarPagamento() {
            when(repository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
            when(repository.save(any(Pagamento.class))).thenReturn(pagamento);

            PagamentoResponse response = pagamentoService.cancelar(pagamentoId);

            assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.CANCELADO);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Deve lançar BusinessException ao cancelar pagamento já pago")
        void deveLancarExcecaoAoCancelarPago() {
            pagamento.confirmar(); // status -> PAGO
            when(repository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));

            assertThatThrownBy(() -> pagamentoService.cancelar(pagamentoId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("confirmado não pode ser cancelado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando pagamento não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagamentoService.cancelar(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
