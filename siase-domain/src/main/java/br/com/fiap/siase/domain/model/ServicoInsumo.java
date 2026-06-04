package br.com.fiap.siase.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ServicoInsumo {
    private UUID servicoId;
    private UUID pecaId;
    private Servico servico;
    private Peca peca;
    private Integer quantidade;

    public ServicoInsumo(Servico servico, Peca peca, Integer quantidade) {
        this.servicoId = servico.getId();
        this.pecaId = peca.getId();
        this.servico = servico;
        this.peca = peca;
        this.quantidade = quantidade;
    }
}
