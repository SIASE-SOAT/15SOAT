package br.com.fiap.siase.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pecas")
public class Peca extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotNull
    @DecimalMin("0.00")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Min(0)
    @Column(name = "quantidade_estoque", nullable = false)
    private Integer quantidadeEstoque = 0;

    @Min(0)
    @Column(name = "estoque_minimo", nullable = false)
    private Integer estoqueMinimo = 0;

    @NotBlank
    @Column(name = "unidade_medida", nullable = false, length = 20)
    private String unidadeMedida = "UN";

    @Column(nullable = false)
    private Boolean ativo = true;

    public boolean temEstoque(int quantidade) {
        return this.quantidadeEstoque >= quantidade;
    }

    public void reservarEstoque(int quantidade) {
        if (!temEstoque(quantidade)) {
            throw new IllegalStateException(
                "Estoque insuficiente para a peça: " + nome + ". Disponível: " + quantidadeEstoque);
        }
        this.quantidadeEstoque -= quantidade;
    }

    public void devolverEstoque(int quantidade) {
        this.quantidadeEstoque += quantidade;
    }
}
