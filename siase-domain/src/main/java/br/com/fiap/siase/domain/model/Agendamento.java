package br.com.fiap.siase.domain.model;

import br.com.fiap.siase.domain.enums.StatusAgendamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "agendamentos")
public class Agendamento extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @Column(name = "data_hora", nullable = false)
    private java.time.LocalDateTime dataHora;

    @Column(name = "descricao_servicos", columnDefinition = "TEXT")
    private String descricaoServicos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_de_servico_id")
    private OrdemDeServico ordemDeServico;

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
