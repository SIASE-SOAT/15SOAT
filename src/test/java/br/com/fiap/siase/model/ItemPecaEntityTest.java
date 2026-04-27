package br.com.fiap.siase.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ItemPeca - Regras de Domínio")
class ItemPecaEntityTest {

    @Test
    @DisplayName("Subtotal deve ser preço unitário multiplicado pela quantidade")
    void deveCalcularSubtotalCorretamente() {
        ItemPeca item = new ItemPeca();
        item.setPrecoUnitario(new BigDecimal("25.00"));
        item.setQuantidade(4);

        assertThat(item.getSubtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Subtotal com quantidade 1 deve ser igual ao preço unitário")
    void subtotalComQuantidadeUmDeveSerIgualAoPreco() {
        ItemPeca item = new ItemPeca();
        item.setPrecoUnitario(new BigDecimal("99.99"));
        item.setQuantidade(1);

        assertThat(item.getSubtotal()).isEqualByComparingTo("99.99");
    }

    @Test
    @DisplayName("Subtotal com preço fracionado deve preservar casas decimais")
    void devePreservarCasasDecimaisNoSubtotal() {
        ItemPeca item = new ItemPeca();
        item.setPrecoUnitario(new BigDecimal("33.33"));
        item.setQuantidade(3);

        assertThat(item.getSubtotal()).isEqualByComparingTo("99.99");
    }
}
