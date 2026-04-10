package br.com.fiap.siase.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "servico_insumos")
public class ServicoInsumo {

    @EmbeddedId
    private ServicoInsumoId id;

    @MapsId("servicoId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @MapsId("pecaId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peca_id", nullable = false)
    private Peca peca;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantidade;

    public ServicoInsumo(Servico servico, Peca peca, Integer quantidade) {
        this.id = new ServicoInsumoId(servico.getId(), peca.getId());
        this.servico = servico;
        this.peca = peca;
        this.quantidade = quantidade;
    }
}
