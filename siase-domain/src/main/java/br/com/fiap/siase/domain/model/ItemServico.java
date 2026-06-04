package br.com.fiap.siase.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemServico {
    private UUID id;
    private OrdemDeServico ordemDeServico;
    private Servico servico;
    private BigDecimal precoUnitario;
    private Integer tempoEstimadoMinutos;
    private LocalDateTime dataInicioExecucao;
    private LocalDateTime dataFimExecucao;
    private String observacoes;
    private LocalDateTime criadoEm;

    public void iniciarExecucao() {
        if (dataInicioExecucao == null) {
            dataInicioExecucao = LocalDateTime.now();
        }
    }

    public void finalizarExecucao() {
        if (dataInicioExecucao == null) {
            iniciarExecucao();
        }
        if (dataFimExecucao == null) {
            dataFimExecucao = LocalDateTime.now();
        }
    }
}
