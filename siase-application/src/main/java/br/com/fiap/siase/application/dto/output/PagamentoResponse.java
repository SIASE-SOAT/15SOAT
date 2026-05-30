package br.com.fiap.siase.application.dto.output;

import br.com.fiap.siase.domain.model.Pagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagamentoResponse(
        UUID id,
        UUID ordemDeServicoId,
        String osNumero,
        String clienteNome,
        String formaPagamento,
        String formaPagamentoDescricao,
        BigDecimal valor,
        String status,
        String statusDescricao,
        LocalDateTime dataPagamento,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static PagamentoResponse from(Pagamento p) {
        return new PagamentoResponse(
                p.getId(),
                p.getOrdemDeServico().getId(),
                p.getOrdemDeServico().getNumero(),
                p.getOrdemDeServico().getCliente().getNome(),
                p.getFormaPagamento().name(),
                p.getFormaPagamento().getDescricao(),
                p.getValor(),
                p.getStatus().name(),
                p.getStatus().getDescricao(),
                p.getDataPagamento(),
                p.getCriadoEm(),
                p.getAtualizadoEm()
        );
    }
}
