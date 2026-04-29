package br.com.fiap.siase.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ItemPeca - Regras de Domínio")
class ItemPecaEntityTest {

    @ParameterizedTest(name = "preço={0}, qtd={1} => subtotal={2}")
    @DisplayName("Subtotal deve ser preço unitário multiplicado pela quantidade")
    @CsvSource({
        "25.00, 4, 100.00",
        "99.99, 1, 99.99",
        "33.33, 3, 99.99"
    })
    void deveCalcularSubtotalCorretamente(String preco, int quantidade, String esperado) {
        ItemPeca item = new ItemPeca();
        item.setPrecoUnitario(new BigDecimal(preco));
        item.setQuantidade(quantidade);

        assertThat(item.getSubtotal()).isEqualByComparingTo(esperado);
    }
}
