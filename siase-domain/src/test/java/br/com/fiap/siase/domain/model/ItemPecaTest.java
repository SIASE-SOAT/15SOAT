package br.com.fiap.siase.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ItemPeca: calculo de subtotal")
class ItemPecaTest {

    @Test
    @DisplayName("Deve calcular subtotal corretamente")
    void deveCalcularSubtotal() {
        ItemPeca item = ItemPeca.builder()
                .quantidade(3)
                .precoUnitario(new BigDecimal("45.90"))
                .build();

        assertThat(item.getSubtotal()).isEqualByComparingTo("137.70");
    }

    @Test
    @DisplayName("Deve calcular subtotal com quantidade 1")
    void deveCalcularSubtotalComQuantidadeUm() {
        ItemPeca item = ItemPeca.builder()
                .quantidade(1)
                .precoUnitario(new BigDecimal("200.00"))
                .build();

        assertThat(item.getSubtotal()).isEqualByComparingTo("200.00");
    }
}
