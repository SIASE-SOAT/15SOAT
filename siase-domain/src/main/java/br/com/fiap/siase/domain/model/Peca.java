package br.com.fiap.siase.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Peca {
    private UUID id;
    private String codigo;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    @Builder.Default
    private Integer quantidadeEstoque = 0;
    @Builder.Default
    private Integer estoqueMinimo = 0;
    @Builder.Default
    private String unidadeMedida = "UN";
    @Builder.Default
    private Boolean ativo = true;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

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
