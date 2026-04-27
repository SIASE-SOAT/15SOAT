package br.com.fiap.siase.model;

import br.com.fiap.siase.model.enums.StatusPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Pagamento - Regras de Domínio")
class PagamentoEntityTest {

    private Pagamento pagamento;

    @BeforeEach
    void setUp() {
        pagamento = new Pagamento();
        pagamento.setValor(new BigDecimal("500.00"));
    }

    @Nested
    @DisplayName("Confirmar")
    class Confirmar {

        @Test
        @DisplayName("Deve confirmar pagamento pendente e registrar data")
        void deveConfirmarPagamentoPendente() {
            pagamento.confirmar();

            assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PAGO);
            assertThat(pagamento.getDataPagamento()).isNotNull();
        }

        @Test
        @DisplayName("Deve rejeitar confirmação de pagamento já pago")
        void deveRejeitarConfirmacaoDeJaPago() {
            pagamento.confirmar();

            assertThatThrownBy(() -> pagamento.confirmar())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("pendentes podem ser confirmados");
        }

        @Test
        @DisplayName("Deve rejeitar confirmação de pagamento cancelado")
        void deveRejeitarConfirmacaoDeCancelado() {
            pagamento.cancelar();

            assertThatThrownBy(() -> pagamento.confirmar())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Cancelar")
    class Cancelar {

        @Test
        @DisplayName("Deve cancelar pagamento pendente")
        void deveCancelarPagamentoPendente() {
            pagamento.cancelar();

            assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.CANCELADO);
        }

        @Test
        @DisplayName("Deve rejeitar cancelamento de pagamento já pago")
        void deveRejeitarCancelamentoDeJaPago() {
            pagamento.confirmar();

            assertThatThrownBy(() -> pagamento.cancelar())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("já confirmado não pode ser cancelado");
        }
    }
}
