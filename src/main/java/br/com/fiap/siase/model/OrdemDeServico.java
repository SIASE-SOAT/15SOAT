package br.com.fiap.siase.model;

import br.com.fiap.siase.model.enums.StatusOS;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ordens_de_servico")
public class OrdemDeServico extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOS status = StatusOS.RECEBIDA;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "total_servicos", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalServicos = BigDecimal.ZERO;

    @Column(name = "total_pecas", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPecas = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    @OneToMany(mappedBy = "ordemDeServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemServico> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "ordemDeServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPeca> itensPeca = new ArrayList<>();

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (dataAbertura == null) {
            dataAbertura = LocalDateTime.now();
        }
    }

    public void recalcularTotais() {
        this.totalServicos = itensServico.stream()
                .map(ItemServico::getPrecoUnitario)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalPecas = itensPeca.stream()
                .map(ItemPeca::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.total = this.totalServicos.add(this.totalPecas);
    }

    public void avancarStatus() {
        this.status = switch (this.status) {
            case RECEBIDA -> StatusOS.EM_DIAGNOSTICO;
            case EM_DIAGNOSTICO -> StatusOS.AGUARDANDO_APROVACAO;
            case AGUARDANDO_APROVACAO -> StatusOS.EM_EXECUCAO;
            case EM_EXECUCAO -> StatusOS.FINALIZADA;
            case FINALIZADA -> StatusOS.ENTREGUE;
            case ENTREGUE -> throw new IllegalStateException("Ordem de serviço já foi entregue.");
        };

        if (this.status == StatusOS.ENTREGUE) {
            this.dataFechamento = LocalDateTime.now();
        }
    }
}
