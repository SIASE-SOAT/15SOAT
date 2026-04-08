package br.com.fiap.siase.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ServicoInsumoId implements Serializable {

    @Column(name = "servico_id")
    private UUID servicoId;

    @Column(name = "peca_id")
    private UUID pecaId;
}
