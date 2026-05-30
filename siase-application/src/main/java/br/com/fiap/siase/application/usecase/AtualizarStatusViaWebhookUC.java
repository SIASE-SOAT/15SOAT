package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.port.EmailPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;

public class AtualizarStatusViaWebhookUC {

    private static final String WEBHOOK_TOKEN = System.getenv().getOrDefault("WEBHOOK_TOKEN", "siase-webhook-token-padrao");

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final EmailPort emailPort;

    public AtualizarStatusViaWebhookUC(OrdemServicoRepositoryPort ordemServicoRepository, EmailPort emailPort) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.emailPort = emailPort;
    }

    public OrdemDeServicoResponse executar(String numero, String novoStatus, String tokenExterno) {
        if (!WEBHOOK_TOKEN.equals(tokenExterno)) {
            throw new BusinessException("Token de serviço externo inválido.");
        }

        var os = ordemServicoRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + numero));

        StatusOS statusAlvo;
        try {
            statusAlvo = StatusOS.valueOf(novoStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status inválido: " + novoStatus);
        }

        if (statusAlvo == StatusOS.CANCELADA) {
            os.cancelar();
        } else if (ehAvancar(os.getStatus(), statusAlvo)) {
            os.avancarStatus();
        } else {
            throw new BusinessException(
                    "Transição de status inválida: " + os.getStatus() + " -> " + statusAlvo);
        }

        var salvo = ordemServicoRepository.save(os);
        notificarCliente(os);
        return OrdemDeServicoResponse.from(salvo);
    }

    private boolean ehAvancar(StatusOS atual, StatusOS alvo) {
        return switch (atual) {
            case RECEBIDA -> alvo == StatusOS.EM_DIAGNOSTICO;
            case EM_DIAGNOSTICO -> alvo == StatusOS.AGUARDANDO_APROVACAO;
            case AGUARDANDO_APROVACAO -> alvo == StatusOS.APROVADO;
            case APROVADO -> alvo == StatusOS.EM_EXECUCAO;
            case EM_EXECUCAO -> alvo == StatusOS.FINALIZADA;
            case FINALIZADA -> alvo == StatusOS.ENTREGUE;
            default -> false;
        };
    }

    private void notificarCliente(br.com.fiap.siase.domain.model.OrdemDeServico os) {
        String email = os.getCliente().getEmail() != null
                ? os.getCliente().getEmail() : "sem-email@siase.com";
        emailPort.enviarOrcamentoAprovado(email, os.getCliente().getNome(), os.getNumero());
    }
}
