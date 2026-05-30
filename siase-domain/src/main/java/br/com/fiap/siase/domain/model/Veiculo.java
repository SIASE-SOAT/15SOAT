package br.com.fiap.siase.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "veiculos")
public class Veiculo extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String marca;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String modelo;

    @NotNull
    @Column(nullable = false)
    private Integer ano;

    @Column(length = 50)
    private String cor;

    @Column(nullable = false)
    private Boolean ativo = true;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
}
