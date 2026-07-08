package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.AvancarStatusUCPort;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.port.EmailPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class AvancarStatusUC implements AvancarStatusUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final EmailPort emailPort;

    public AvancarStatusUC(OrdemServicoRepositoryPort ordemServicoRepository, EmailPort emailPort) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.emailPort = emailPort;
    }

    @Override
    public OrdemDeServicoResponse executar(UUID osId) {
        OrdemDeServico os = ordemServicoRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + osId));
        try {
            os.avancarStatus();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        var salvo = ordemServicoRepository.save(os);

        if (salvo.getStatus() == StatusOS.AGUARDANDO_APROVACAO) {
            String email = salvo.getCliente().getEmail() != null
                    ? salvo.getCliente().getEmail() : "sem-email@siase.com";
            log.info("[AvancarStatusUC] Disparando email de orçamento para aprovação | OS: {} | Para: {}",
                    salvo.getNumero(), email);
            emailPort.enviarOrcamentoParaAprovacao(
                    email, salvo.getCliente().getNome(), salvo.getNumero(), salvo.getTotal().toPlainString());
        }

        return OrdemDeServicoResponse.from(salvo);
    }
}
