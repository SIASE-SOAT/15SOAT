package br.com.fiap.siase.domain.model;

import br.com.fiap.siase.domain.enums.FormaPagamento;
import br.com.fiap.siase.domain.enums.StatusPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pagamento: regras de negocio")
class PagamentoTest {

    private Pagamento pagamento;

    @BeforeEach
    void setUp() {
        pagamento = Pagamento.builder()
                .formaPagamento(FormaPagamento.PIX)
                .valor(new BigDecimal("500.00"))
                .build();
    }

    @Test
    @DisplayName("Status inicial deve ser PENDENTE")
    void statusInicialDevePendente() {
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
    }

    @Test
    @DisplayName("Deve confirmar pagamento pendente e registrar data")
    void deveConfirmarPagamentoPendente() {
        pagamento.confirmar();
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PAGO);
        assertThat(pagamento.getDataPagamento()).isNotNull();
    }

    @Test
    @DisplayName("Nao deve confirmar pagamento ja pago")
    void naoDeveConfirmarPagamentoJaPago() {
        pagamento.confirmar();
        assertThatThrownBy(() -> pagamento.confirmar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pendentes");
    }

    @Test
    @DisplayName("Deve cancelar pagamento pendente")
    void deveCancelarPagamentoPendente() {
        pagamento.cancelar();
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.CANCELADO);
    }

    @Test
    @DisplayName("Nao deve cancelar pagamento ja pago")
    void naoDeveCancelarPagamentoJaPago() {
        pagamento.confirmar();
        assertThatThrownBy(() -> pagamento.cancelar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("confirmado");
    }
}
