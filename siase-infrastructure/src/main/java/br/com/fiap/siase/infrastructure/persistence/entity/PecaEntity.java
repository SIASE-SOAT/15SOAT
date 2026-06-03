package br.com.fiap.siase.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pecas")
public class PecaEntity extends BaseJpaEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_estoque", nullable = false)
    private Integer quantidadeEstoque = 0;

    @Column(name = "estoque_minimo", nullable = false)
    private Integer estoqueMinimo = 0;

    @Column(name = "unidade_medida", nullable = false, length = 20)
    private String unidadeMedida = "UN";

    @Column(nullable = false)
    private Boolean ativo = true;
}
