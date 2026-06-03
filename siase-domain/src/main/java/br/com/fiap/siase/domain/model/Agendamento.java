package br.com.fiap.siase.domain.model;

import br.com.fiap.siase.domain.enums.StatusAgendamento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agendamento {
    private UUID id;
    private Cliente cliente;
    private Veiculo veiculo;
    private LocalDateTime dataHora;
    private String descricaoServicos;
    @Builder.Default
    private StatusAgendamento status = StatusAgendamento.AGENDADO;
    private OrdemDeServico ordemDeServico;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public void confirmar() {
        if (this.status != StatusAgendamento.AGENDADO) {
            throw new IllegalStateException("Apenas agendamentos com status AGENDADO podem ser confirmados.");
        }
        this.status = StatusAgendamento.CONFIRMADO;
    }

    public void cancelar() {
        if (this.status == StatusAgendamento.REALIZADO) {
            throw new IllegalStateException("Agendamento já realizado não pode ser cancelado.");
        }
        this.status = StatusAgendamento.CANCELADO;
    }

    public void realizar(OrdemDeServico os) {
        if (this.status != StatusAgendamento.CONFIRMADO) {
            throw new IllegalStateException("Apenas agendamentos CONFIRMADOS podem ser realizados.");
        }
        this.status = StatusAgendamento.REALIZADO;
        this.ordemDeServico = os;
    }
}
