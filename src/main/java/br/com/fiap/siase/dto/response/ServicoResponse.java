package br.com.fiap.siase.dto.response;

import br.com.fiap.siase.model.Servico;
import br.com.fiap.siase.model.ServicoInsumo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ServicoResponse(
        UUID id,
        String nome,
        String descricao,
        BigDecimal preco,
        Boolean ativo,
        List<InsumoResponse> insumos,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public record InsumoResponse(
            UUID pecaId,
            String codigoPeca,
            String nomePeca,
            String unidadeMedida,
            Integer quantidade
    ) {
        public static InsumoResponse from(ServicoInsumo si) {
            return new InsumoResponse(
                    si.getPeca().getId(),
                    si.getPeca().getCodigo(),
                    si.getPeca().getNome(),
                    si.getPeca().getUnidadeMedida(),
                    si.getQuantidade()
            );
        }
    }

    public static ServicoResponse from(Servico s) {
        return new ServicoResponse(
                s.getId(),
                s.getNome(),
                s.getDescricao(),
                s.getPreco(),
                s.getAtivo(),
                s.getInsumos().stream().map(InsumoResponse::from).toList(),
                s.getCriadoEm(),
                s.getAtualizadoEm()
        );
    }
}
