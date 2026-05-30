package br.com.fiap.siase.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Peca: gerenciamento de estoque")
class PecaTest {

    @Test
    @DisplayName("Deve reservar estoque com sucesso")
    void reservarEstoqueComSucesso() {
        var peca = criarPeca(10);

        peca.reservarEstoque(3);

        assertThat(peca.getQuantidadeEstoque()).isEqualTo(7);
    }

    @Test
    @DisplayName("Deve lancar erro ao reservar mais que o estoque disponivel")
    void reservarEstoqueInsuficiente() {
        var peca = criarPeca(5);

        assertThatThrownBy(() -> peca.reservarEstoque(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Estoque insuficiente");
    }

    @Test
    @DisplayName("Deve devolver estoque corretamente")
    void devolverEstoqueCorretamente() {
        var peca = criarPeca(10);
        peca.reservarEstoque(5);

        peca.devolverEstoque(5);

        assertThat(peca.getQuantidadeEstoque()).isEqualTo(10);
    }

    @Test
    @DisplayName("temEstoque retorna true quando ha estoque suficiente")
    void temEstoqueSuficiente() {
        var peca = criarPeca(10);
        assertThat(peca.temEstoque(5)).isTrue();
        assertThat(peca.temEstoque(10)).isTrue();
    }

    @Test
    @DisplayName("temEstoque retorna false quando nao ha estoque suficiente")
    void temEstoqueInsuficiente() {
        var peca = criarPeca(3);
        assertThat(peca.temEstoque(5)).isFalse();
    }

    private Peca criarPeca(int quantidadeEstoque) {
        var peca = new Peca();
        peca.setCodigo("PEC-001");
        peca.setNome("Filtro de Oleo");
        peca.setPreco(new BigDecimal("45.90"));
        peca.setQuantidadeEstoque(quantidadeEstoque);
        peca.setUnidadeMedida("UN");
        peca.setAtivo(true);
        return peca;
    }
}
