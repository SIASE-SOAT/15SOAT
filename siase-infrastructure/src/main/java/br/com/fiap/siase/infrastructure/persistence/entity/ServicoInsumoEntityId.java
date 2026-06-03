package br.com.fiap.siase.infrastructure.persistence.entity;

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
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ServicoInsumoEntityId implements Serializable {

    @Column(name = "servico_id")
    private UUID servicoId;

    @Column(name = "peca_id")
    private UUID pecaId;
}
