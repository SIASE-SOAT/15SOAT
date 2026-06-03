package br.com.fiap.siase.domain.model;

import br.com.fiap.siase.domain.enums.TipoPessoa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {
    private UUID id;
    private String nome;
    private TipoPessoa tipoPessoa;
    private String documento;
    private String email;
    private String telefone;
    private String endereco;
    @Builder.Default
    private Boolean ativo = true;
    @Builder.Default
    private List<Veiculo> veiculos = new ArrayList<>();
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
