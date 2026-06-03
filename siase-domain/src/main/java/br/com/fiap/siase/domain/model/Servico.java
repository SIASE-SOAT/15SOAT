package br.com.fiap.siase.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servico {
    private UUID id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private Integer tempoEstimadoMinutos;
    @Builder.Default
    private Boolean ativo = true;
    @Builder.Default
    private List<ServicoInsumo> insumos = new ArrayList<>();
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
