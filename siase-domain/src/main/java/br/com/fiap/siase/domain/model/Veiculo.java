package br.com.fiap.siase.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Veiculo {
    private UUID id;
    private String placa;
    private String marca;
    private String modelo;
    private Integer ano;
    private String cor;
    @Builder.Default
    private Boolean ativo = true;
    private Cliente cliente;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
