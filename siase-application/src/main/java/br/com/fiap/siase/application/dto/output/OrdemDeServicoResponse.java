package br.com.fiap.siase.application.dto.output;

import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.model.ItemServico;
import br.com.fiap.siase.domain.model.ItemPeca;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrdemDeServicoResponse(
        UUID id,
        String numero,
        UUID clienteId,
        String clienteNome,
        UUID veiculoId,
        String veiculoPlaca,
        String veiculoModelo,
        String status,
        String statusDescricao,
        String observacoes,
        List<ItemServicoResponse> itensServico,
        List<ItemPecaResponse> itensPeca,
        BigDecimal totalServicos,
        BigDecimal totalPecas,
        BigDecimal total,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {

    public record ItemServicoResponse(
            UUID id,
            UUID servicoId,
            String servicoNome,
            BigDecimal precoUnitario,
            Integer tempoEstimadoMinutos,
            String observacoes,
            LocalDateTime dataInicioExecucao,
            LocalDateTime dataFimExecucao
    ) {
        public static ItemServicoResponse from(ItemServico item) {
            return new ItemServicoResponse(
                    item.getId(),
                    item.getServico().getId(),
                    item.getServico().getNome(),
                    item.getPrecoUnitario(),
                    item.getTempoEstimadoMinutos(),
                    item.getObservacoes(),
                    item.getDataInicioExecucao(),
                    item.getDataFimExecucao()
            );
        }
    }

    public record ItemPecaResponse(
            UUID id,
            UUID pecaId,
            String pecaCodigo,
            String pecaNome,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {
        public static ItemPecaResponse from(ItemPeca item) {
            return new ItemPecaResponse(
                    item.getId(),
                    item.getPeca().getId(),
                    item.getPeca().getCodigo(),
                    item.getPeca().getNome(),
                    item.getQuantidade(),
                    item.getPrecoUnitario(),
                    item.getSubtotal()
            );
        }
    }

    public static OrdemDeServicoResponse from(OrdemDeServico os) {
        return new OrdemDeServicoResponse(
                os.getId(),
                os.getNumero(),
                os.getCliente().getId(),
                os.getCliente().getNome(),
                os.getVeiculo().getId(),
                os.getVeiculo().getPlaca(),
                os.getVeiculo().getModelo(),
                os.getStatus().name(),
                os.getStatus().getDescricao(),
                os.getObservacoes(),
                os.getItensServico().stream().map(ItemServicoResponse::from).toList(),
                os.getItensPeca().stream().map(ItemPecaResponse::from).toList(),
                os.getTotalServicos(),
                os.getTotalPecas(),
                os.getTotal(),
                os.getDataAbertura(),
                os.getDataFechamento(),
                os.getCriadoEm(),
                os.getAtualizadoEm()
        );
    }
}
