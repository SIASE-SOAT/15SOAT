package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.PagamentoRequest;
import br.com.fiap.siase.domain.enums.FormaPagamento;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.enums.StatusPagamento;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.model.Pagamento;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.EmailPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.PagamentoRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PagamentoController - Endpoints REST")
class PagamentoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PagamentoRepositoryPort pagamentoRepository;
    @MockBean OrdemServicoRepositoryPort ordemServicoRepository;
    @MockBean EmailPort emailPort;

    private static final UUID OS_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");
    private static final UUID PAGAMENTO_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Cliente cliente;
    private OrdemDeServico osFinalizada;
    private OrdemDeServico osNaoFinalizada;
    private Pagamento pagamentoPendente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(CLIENTE_ID);
        cliente.setNome("João Silva");
        cliente.setEmail("joao@email.com");

        Veiculo veiculo = new Veiculo();
        veiculo.setId(UUID.randomUUID());

        osFinalizada = new OrdemDeServico();
        osFinalizada.setId(OS_ID);
        osFinalizada.setNumero("OS-20240101-ABC123");
        osFinalizada.setCliente(cliente);
        osFinalizada.setVeiculo(veiculo);
        osFinalizada.setStatus(StatusOS.FINALIZADA);

        osNaoFinalizada = new OrdemDeServico();
        osNaoFinalizada.setId(UUID.randomUUID());
        osNaoFinalizada.setNumero("OS-20240101-DEF456");
        osNaoFinalizada.setCliente(cliente);
        osNaoFinalizada.setVeiculo(veiculo);
        osNaoFinalizada.setStatus(StatusOS.EM_EXECUCAO);

        pagamentoPendente = new Pagamento();
        pagamentoPendente.setId(PAGAMENTO_ID);
        pagamentoPendente.setOrdemDeServico(osFinalizada);
        pagamentoPendente.setFormaPagamento(FormaPagamento.PIX);
        pagamentoPendente.setValor(new BigDecimal("350.00"));
        pagamentoPendente.setStatus(StatusPagamento.PENDENTE);
        pagamentoPendente.setCriadoEm(LocalDateTime.now());
        pagamentoPendente.setAtualizadoEm(LocalDateTime.now());
    }

    @Nested
    @DisplayName("POST /ordens/{osId}/pagamento - Registrar Pagamento")
    class RegistrarPagamento {

        @Test
        @DisplayName("deve registrar pagamento e retornar 201")
        void deveRegistrarPagamento() throws Exception {
            PagamentoRequest request = new PagamentoRequest(FormaPagamento.PIX, new BigDecimal("350.00"));

            when(ordemServicoRepository.findById(OS_ID)).thenReturn(Optional.of(osFinalizada));
            when(pagamentoRepository.existsByOrdemDeServicoId(OS_ID)).thenReturn(false);
            when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
                Pagamento p = invocation.getArgument(0);
                p.setId(PAGAMENTO_ID);
                p.setCriadoEm(LocalDateTime.now());
                p.setAtualizadoEm(LocalDateTime.now());
                return p;
            });

            mockMvc.perform(post("/ordens/{osId}/pagamento", OS_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(PAGAMENTO_ID.toString()))
                    .andExpect(jsonPath("$.ordemDeServicoId").value(OS_ID.toString()))
                    .andExpect(jsonPath("$.status").value("PENDENTE"));
        }

        @Test
        @DisplayName("deve retornar 404 quando OS não encontrada")
        void deveRetornar404QuandoOSNaoEncontrada() throws Exception {
            PagamentoRequest request = new PagamentoRequest(FormaPagamento.PIX, new BigDecimal("350.00"));
            UUID invalidOsId = UUID.randomUUID();

            when(ordemServicoRepository.findById(invalidOsId)).thenReturn(Optional.empty());

            mockMvc.perform(post("/ordens/{osId}/pagamento", invalidOsId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 422 quando OS não está FINALIZADA")
        void deveRetornar422QuandoOSNaoFinalizada() throws Exception {
            PagamentoRequest request = new PagamentoRequest(FormaPagamento.PIX, new BigDecimal("350.00"));
            UUID osId = UUID.randomUUID();

            when(ordemServicoRepository.findById(osId)).thenReturn(Optional.of(osNaoFinalizada));

            mockMvc.perform(post("/ordens/{osId}/pagamento", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(
                            "Pagamento só pode ser registrado para OS com status FINALIZADA. Status atual: "
                                    + osNaoFinalizada.getStatus().getDescricao()));
        }

        @Test
        @DisplayName("deve retornar 422 quando OS já possui pagamento")
        void deveRetornar422QuandoOSJaPossuiPagamento() throws Exception {
            PagamentoRequest request = new PagamentoRequest(FormaPagamento.PIX, new BigDecimal("350.00"));

            when(ordemServicoRepository.findById(OS_ID)).thenReturn(Optional.of(osFinalizada));
            when(pagamentoRepository.existsByOrdemDeServicoId(OS_ID)).thenReturn(true);

            mockMvc.perform(post("/ordens/{osId}/pagamento", OS_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Esta OS já possui um pagamento registrado."));
        }

        @Test
        @DisplayName("deve retornar 400 quando dados inválidos")
        void deveRetornar400ComDadosInvalidos() throws Exception {
            PagamentoRequest request = new PagamentoRequest(null, null);

            mockMvc.perform(post("/ordens/{osId}/pagamento", OS_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /ordens/{osId}/pagamento - Buscar Pagamento por OS")
    class BuscarPagamentoPorOS {

        @Test
        @DisplayName("deve retornar 200 com pagamento")
        void deveRetornarPagamento() throws Exception {
            when(pagamentoRepository.findByOrdemDeServicoId(OS_ID))
                    .thenReturn(Optional.of(pagamentoPendente));

            mockMvc.perform(get("/ordens/{osId}/pagamento", OS_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PAGAMENTO_ID.toString()))
                    .andExpect(jsonPath("$.ordemDeServicoId").value(OS_ID.toString()))
                    .andExpect(jsonPath("$.clienteNome").value("João Silva"))
                    .andExpect(jsonPath("$.status").value("PENDENTE"));
        }

        @Test
        @DisplayName("deve retornar 404 quando pagamento não encontrado")
        void deveRetornar404() throws Exception {
            when(pagamentoRepository.findByOrdemDeServicoId(OS_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/ordens/{osId}/pagamento", OS_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /pagamentos/{id}/confirmar - Confirmar Pagamento")
    class ConfirmarPagamento {

        @Test
        @DisplayName("deve confirmar pagamento e retornar 200")
        void deveConfirmarPagamento() throws Exception {
            when(pagamentoRepository.findById(PAGAMENTO_ID))
                    .thenReturn(Optional.of(pagamentoPendente));
            when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
                Pagamento p = invocation.getArgument(0);
                p.setAtualizadoEm(LocalDateTime.now());
                return p;
            });
            when(ordemServicoRepository.save(any(OrdemDeServico.class))).thenAnswer(invocation -> {
                OrdemDeServico os = invocation.getArgument(0);
                return os;
            });
            doNothing().when(emailPort).enviarConfirmacaoPagamento(any(), any(), any(), any(), any());

            mockMvc.perform(patch("/pagamentos/{id}/confirmar", PAGAMENTO_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PAGO"))
                    .andExpect(jsonPath("$.dataPagamento", notNullValue()));
        }

        @Test
        @DisplayName("deve retornar 404 quando pagamento não encontrado")
        void deveRetornar404() throws Exception {
            when(pagamentoRepository.findById(PAGAMENTO_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(patch("/pagamentos/{id}/confirmar", PAGAMENTO_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 422 quando pagamento não está PENDENTE")
        void deveRetornar422QuandoNaoPendente() throws Exception {
            Pagamento pagamentoPago = new Pagamento();
            pagamentoPago.setId(PAGAMENTO_ID);
            pagamentoPago.setOrdemDeServico(osFinalizada);
            pagamentoPago.setFormaPagamento(FormaPagamento.PIX);
            pagamentoPago.setValor(new BigDecimal("350.00"));
            pagamentoPago.setStatus(StatusPagamento.PAGO);
            pagamentoPago.setCriadoEm(LocalDateTime.now());
            pagamentoPago.setAtualizadoEm(LocalDateTime.now());

            when(pagamentoRepository.findById(PAGAMENTO_ID))
                    .thenReturn(Optional.of(pagamentoPago));

            mockMvc.perform(patch("/pagamentos/{id}/confirmar", PAGAMENTO_ID))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("PATCH /pagamentos/{id}/cancelar - Cancelar Pagamento")
    class CancelarPagamento {

        @Test
        @DisplayName("deve cancelar pagamento e retornar 200")
        void deveCancelarPagamento() throws Exception {
            when(pagamentoRepository.findById(PAGAMENTO_ID))
                    .thenReturn(Optional.of(pagamentoPendente));
            when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
                Pagamento p = invocation.getArgument(0);
                p.setAtualizadoEm(LocalDateTime.now());
                return p;
            });

            mockMvc.perform(patch("/pagamentos/{id}/cancelar", PAGAMENTO_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"));
        }

        @Test
        @DisplayName("deve retornar 404 quando pagamento não encontrado")
        void deveRetornar404() throws Exception {
            when(pagamentoRepository.findById(PAGAMENTO_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(patch("/pagamentos/{id}/cancelar", PAGAMENTO_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 422 quando pagamento já está PAGO")
        void deveRetornar422QuandoJaPago() throws Exception {
            Pagamento pagamentoPago = new Pagamento();
            pagamentoPago.setId(PAGAMENTO_ID);
            pagamentoPago.setOrdemDeServico(osFinalizada);
            pagamentoPago.setFormaPagamento(FormaPagamento.PIX);
            pagamentoPago.setValor(new BigDecimal("350.00"));
            pagamentoPago.setStatus(StatusPagamento.PAGO);
            pagamentoPago.setCriadoEm(LocalDateTime.now());
            pagamentoPago.setAtualizadoEm(LocalDateTime.now());

            when(pagamentoRepository.findById(PAGAMENTO_ID))
                    .thenReturn(Optional.of(pagamentoPago));

            mockMvc.perform(patch("/pagamentos/{id}/cancelar", PAGAMENTO_ID))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
