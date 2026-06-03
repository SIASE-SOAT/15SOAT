package br.com.fiap.siase.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "itens_servico")
public class ItemServicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_de_servico_id", nullable = false)
    private OrdemDeServicoEntity ordemDeServico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id", nullable = false)
    private ServicoEntity servico;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "tempo_estimado_minutos")
    private Integer tempoEstimadoMinutos;

    @Column(name = "data_inicio_execucao")
    private LocalDateTime dataInicioExecucao;

    @Column(name = "data_fim_execucao")
    private LocalDateTime dataFimExecucao;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "criado_em", updatable = false, nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
    }
}
