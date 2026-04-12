package br.com.fiap.siase.dto.response;

import br.com.fiap.siase.model.PedidoCompra;

import java.time.LocalDateTime;
import java.util.UUID;

public record PedidoCompraResponse(
        UUID id,
        UUID pecaId,
        String pecaCodigo,
        String pecaNome,
        Integer quantidadeSolicitada,
        Integer quantidadeRecebida,
        String status,
        String statusDescricao,
        String observacoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static PedidoCompraResponse from(PedidoCompra p) {
        return new PedidoCompraResponse(
                p.getId(),
                p.getPeca().getId(),
                p.getPeca().getCodigo(),
                p.getPeca().getNome(),
                p.getQuantidadeSolicitada(),
                p.getQuantidadeRecebida(),
                p.getStatus().name(),
                p.getStatus().getDescricao(),
                p.getObservacoes(),
                p.getCriadoEm(),
                p.getAtualizadoEm()
        );
    }
}
