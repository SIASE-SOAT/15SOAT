package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.AprovarOrcamentoUCPort;
import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.port.EmailPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;

public class AprovarOrcamentoUC implements AprovarOrcamentoUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final EmailPort emailPort;

    public AprovarOrcamentoUC(OrdemServicoRepositoryPort ordemServicoRepository, EmailPort emailPort) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.emailPort = emailPort;
    }

    public OrdemDeServicoResponse aprovar(String numero) {
        var os = ordemServicoRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + numero));

        if (os.getStatus() != StatusOS.AGUARDANDO_APROVACAO) {
            throw new BusinessException("A OS " + numero + " não está aguardando aprovação no momento.");
        }

        os.avancarStatus();
        var salvo = ordemServicoRepository.save(os);

        String email = os.getCliente().getEmail() != null
                ? os.getCliente().getEmail() : "sem-email@siase.com";
        emailPort.enviarOrcamentoAprovado(email, os.getCliente().getNome(), os.getNumero());

        return OrdemDeServicoResponse.from(salvo);
    }

    public OrdemDeServicoResponse recusar(String numero) {
        var os = ordemServicoRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + numero));

        if (os.getStatus() != StatusOS.AGUARDANDO_APROVACAO) {
            throw new BusinessException("A OS " + numero + " não está aguardando aprovação no momento.");
        }

        os.cancelar();
        var salvo = ordemServicoRepository.save(os);

        String email = os.getCliente().getEmail() != null
                ? os.getCliente().getEmail() : "sem-email@siase.com";
        emailPort.enviarOrcamentoCancelado(email, os.getCliente().getNome(), os.getNumero());

        return OrdemDeServicoResponse.from(salvo);
    }
}
