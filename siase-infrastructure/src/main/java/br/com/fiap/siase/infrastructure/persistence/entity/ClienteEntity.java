package br.com.fiap.siase.infrastructure.persistence.entity;

import br.com.fiap.siase.domain.enums.TipoPessoa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clientes")
public class ClienteEntity extends BaseJpaEntity {

    @Column(nullable = false, length = 255)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 2)
    private TipoPessoa tipoPessoa;

    @Column(nullable = false, unique = true, length = 18)
    private String documento;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(length = 500)
    private String endereco;

    @Column(nullable = false)
    private Boolean ativo = true;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VeiculoEntity> veiculos = new ArrayList<>();
}
