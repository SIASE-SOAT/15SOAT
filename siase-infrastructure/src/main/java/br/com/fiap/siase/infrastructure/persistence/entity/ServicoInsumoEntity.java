package br.com.fiap.siase.infrastructure.persistence.entity;

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
public class ServicoInsumoEntity {

    @EmbeddedId
    private ServicoInsumoEntityId id;

    @MapsId("servicoId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id")
    private ServicoEntity servico;

    @MapsId("pecaId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peca_id")
    private PecaEntity peca;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantidade;
}
