package br.com.fiap.siase.application.dto.output;

import br.com.fiap.siase.domain.model.Peca;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PecaResponse(
        UUID id,
        String codigo,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer quantidadeEstoque,
        Integer estoqueMinimo,
        String unidadeMedida,
        Boolean ativo,
        Boolean estoqueAbaixoMinimo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static PecaResponse from(Peca p) {
        return new PecaResponse(
                p.getId(),
                p.getCodigo(),
                p.getNome(),
                p.getDescricao(),
                p.getPreco(),
                p.getQuantidadeEstoque(),
                p.getEstoqueMinimo(),
                p.getUnidadeMedida(),
                p.getAtivo(),
                p.getQuantidadeEstoque() < p.getEstoqueMinimo(),
                p.getCriadoEm(),
                p.getAtualizadoEm()
        );
    }
}
