package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.AtualizarStatusViaWebhookUCPort;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.port.EmailPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AtualizarStatusViaWebhookUC implements AtualizarStatusViaWebhookUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final EmailPort emailPort;
    private final String webhookToken;

    public AtualizarStatusViaWebhookUC(OrdemServicoRepositoryPort ordemServicoRepository, EmailPort emailPort, String webhookToken) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.emailPort = emailPort;
        this.webhookToken = webhookToken;
    }

    public OrdemDeServicoResponse executar(String numero, String novoStatus, String tokenExterno) {
        if (!webhookToken.equals(tokenExterno)) {
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
        } else if (os.podeAvancarPara(statusAlvo)) {
            os.avancarStatus();
        } else {
            throw new BusinessException(
                    "Transição de status inválida: " + os.getStatus() + " -> " + statusAlvo);
        }

        var salvo = ordemServicoRepository.save(os);
        notificarCliente(os);
        return OrdemDeServicoResponse.from(salvo);
    }

    private void notificarCliente(OrdemDeServico os) {
        String email = os.getCliente().getEmail() != null
                ? os.getCliente().getEmail() : "sem-email@siase.com";
        String nome = os.getCliente().getNome();
        String numero = os.getNumero();

        log.info("[AtualizarStatusViaWebhookUC] Status atualizado para {} | OS: {} | Notificando: {}", os.getStatus(), numero, email);
        switch (os.getStatus()) {
            case APROVADO -> emailPort.enviarOrcamentoAprovado(email, nome, numero);
            case CANCELADA -> emailPort.enviarOrcamentoCancelado(email, nome, numero);
            default -> log.info("[AtualizarStatusViaWebhookUC] Status {} não dispara notificação por email", os.getStatus());
        }
    }
}
