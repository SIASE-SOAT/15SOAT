package br.com.fiap.siase.controller;

import br.com.fiap.siase.config.SecurityConfig;
import br.com.fiap.siase.dto.response.PagamentoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.GlobalExceptionHandler;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.enums.FormaPagamento;
import br.com.fiap.siase.model.enums.StatusPagamento;
import br.com.fiap.siase.security.JwtService;
import br.com.fiap.siase.security.UserDetailsServiceImpl;
import br.com.fiap.siase.service.PagamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PagamentoController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
@DisplayName("PagamentoController - Endpoints REST")
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagamentoService service;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private UUID osId;
    private UUID pagamentoId;
    private PagamentoResponse response;

    @BeforeEach
    void setUp() {
        osId = UUID.randomUUID();
        pagamentoId = UUID.randomUUID();
        response = new PagamentoResponse(
                pagamentoId,
                osId,
                "OS-001",
                "Maria Oliveira",
                FormaPagamento.PIX.name(),
                FormaPagamento.PIX.getDescricao(),
                new BigDecimal("500.00"),
                StatusPagamento.PENDENTE.name(),
                StatusPagamento.PENDENTE.getDescricao(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /ordens/{osId}/pagamento")
    class Registrar {

        @Test
        @DisplayName("Deve retornar 201 com Location")
        void deveRegistrarPagamento() throws Exception {
            when(service.registrar(any(), any())).thenReturn(response);

            mockMvc.perform(post("/ordens/{osId}/pagamento", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "formaPagamento": "PIX",
                                      "valor": 500.00
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(pagamentoId.toString())))
                    .andExpect(jsonPath("$.ordemDeServicoId", is(osId.toString())))
                    .andExpect(jsonPath("$.formaPagamento", is("PIX")));
        }

        @Test
        @DisplayName("Deve retornar 400 para payload inválido")
        void deveRetornar400ParaPayloadInvalido() throws Exception {
            mockMvc.perform(post("/ordens/{osId}/pagamento", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "valor": 0
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET/PATCH pagamento")
    class LeituraETransicoes {

        @Test
        @DisplayName("Deve buscar pagamento por OS")
        void deveBuscarPorOS() throws Exception {
            when(service.buscarPorOS(osId)).thenReturn(response);

            mockMvc.perform(get("/ordens/{osId}/pagamento", osId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clienteNome", is("Maria Oliveira")));
        }

        @Test
        @DisplayName("Deve confirmar pagamento")
        void deveConfirmarPagamento() throws Exception {
            PagamentoResponse confirmado = new PagamentoResponse(
                    pagamentoId,
                    osId,
                    "OS-001",
                    "Maria Oliveira",
                    "PIX",
                    "Pix",
                    new BigDecimal("500.00"),
                    "PAGO",
                    "Pago",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            when(service.confirmar(pagamentoId)).thenReturn(confirmado);

            mockMvc.perform(patch("/pagamentos/{id}/confirmar", pagamentoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PAGO")));
        }

        @Test
        @DisplayName("Deve cancelar pagamento")
        void deveCancelarPagamento() throws Exception {
            PagamentoResponse cancelado = new PagamentoResponse(
                    pagamentoId,
                    osId,
                    "OS-001",
                    "Maria Oliveira",
                    "PIX",
                    "Pix",
                    new BigDecimal("500.00"),
                    "CANCELADO",
                    "Cancelado",
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            when(service.cancelar(pagamentoId)).thenReturn(cancelado);

            mockMvc.perform(patch("/pagamentos/{id}/cancelar", pagamentoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELADO")));
        }

        @Test
        @DisplayName("Deve retornar 404 quando pagamento não existe")
        void deveRetornar404QuandoNaoExiste() throws Exception {
            when(service.buscarPorOS(osId)).thenThrow(new ResourceNotFoundException("Pagamento não encontrado"));

            mockMvc.perform(get("/ordens/{osId}/pagamento", osId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 422 em regra de negócio")
        void deveRetornar422EmRegraDeNegocio() throws Exception {
            when(service.confirmar(pagamentoId))
                    .thenThrow(new BusinessException("Somente pagamentos pendentes podem ser confirmados."));

            mockMvc.perform(patch("/pagamentos/{id}/confirmar", pagamentoId))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("pendentes")));
        }
    }
}
