package br.com.fiap.siase.application.dto.output;

import br.com.fiap.siase.domain.model.Agendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoResponse(
        UUID id,
        UUID clienteId,
        String clienteNome,
        String clienteEmail,
        UUID veiculoId,
        String veiculoPlaca,
        String veiculoModelo,
        LocalDateTime dataHora,
        String descricaoServicos,
        String status,
        String statusDescricao,
        UUID ordemDeServicoId,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static AgendamentoResponse from(Agendamento a) {
        return new AgendamentoResponse(
                a.getId(),
                a.getCliente().getId(),
                a.getCliente().getNome(),
                a.getCliente().getEmail(),
                a.getVeiculo().getId(),
                a.getVeiculo().getPlaca(),
                a.getVeiculo().getModelo(),
                a.getDataHora(),
                a.getDescricaoServicos(),
                a.getStatus().name(),
                a.getStatus().getDescricao(),
                a.getOrdemDeServico() != null ? a.getOrdemDeServico().getId() : null,
                a.getCriadoEm(),
                a.getAtualizadoEm()
        );
    }
}
