package br.com.fiap.siase.domain.model;

import br.com.fiap.siase.domain.enums.StatusOS;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdemDeServico {
    private UUID id;
    private String numero;
    private Cliente cliente;
    private Veiculo veiculo;
    @Builder.Default
    private StatusOS status = StatusOS.RECEBIDA;
    private String observacoes;
    @Builder.Default
    private BigDecimal totalServicos = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalPecas = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataFechamento;
    @Builder.Default
    private List<ItemServico> itensServico = new ArrayList<>();
    @Builder.Default
    private List<ItemPeca> itensPeca = new ArrayList<>();
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static String gerarNumero() {
        String data = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sufixo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "OS-" + data + "-" + sufixo;
    }

    public boolean podeAvancarPara(StatusOS alvo) {
        return switch (this.status) {
            case RECEBIDA -> alvo == StatusOS.EM_DIAGNOSTICO;
            case EM_DIAGNOSTICO -> alvo == StatusOS.AGUARDANDO_APROVACAO;
            case AGUARDANDO_APROVACAO -> alvo == StatusOS.APROVADO;
            case APROVADO -> alvo == StatusOS.EM_EXECUCAO;
            case EM_EXECUCAO -> alvo == StatusOS.FINALIZADA;
            case FINALIZADA -> alvo == StatusOS.ENTREGUE;
            default -> false;
        };
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
            case AGUARDANDO_APROVACAO -> StatusOS.APROVADO;
            case APROVADO -> StatusOS.EM_EXECUCAO;
            case EM_EXECUCAO -> StatusOS.FINALIZADA;
            case FINALIZADA -> StatusOS.ENTREGUE;
            case ENTREGUE -> throw new IllegalStateException("Ordem de serviço já foi entregue.");
            case CANCELADA -> throw new IllegalStateException("Ordem de serviço cancelada não pode ser avançada.");
        };
        if (this.status == StatusOS.ENTREGUE) {
            this.dataFechamento = LocalDateTime.now();
        }
    }

    public void cancelar() {
        if (this.status == StatusOS.ENTREGUE) {
            throw new IllegalStateException("Ordem de serviço já entregue não pode ser cancelada.");
        }
        if (this.status == StatusOS.CANCELADA) {
            throw new IllegalStateException("Ordem de serviço já está cancelada.");
        }
        if (this.status == StatusOS.EM_EXECUCAO || this.status == StatusOS.FINALIZADA) {
            throw new IllegalStateException("Ordem de serviço em execução ou finalizada não pode ser cancelada. Entre em contato com a oficina.");
        }
        this.status = StatusOS.CANCELADA;
        this.dataFechamento = LocalDateTime.now();
    }
}
