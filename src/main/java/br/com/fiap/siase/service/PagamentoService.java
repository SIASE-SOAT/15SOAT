package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.PagamentoRequest;
import br.com.fiap.siase.dto.response.PagamentoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Pagamento;
import br.com.fiap.siase.model.enums.StatusOS;
import br.com.fiap.siase.repository.OrdemDeServicoRepository;
import br.com.fiap.siase.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository repository;
    private final OrdemDeServicoRepository osRepository;
    private final EmailService emailService;

    @Transactional
    public PagamentoResponse registrar(UUID osId, PagamentoRequest request) {
        var os = osRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + osId));

        if (os.getStatus() != StatusOS.FINALIZADA) {
            throw new BusinessException(
                "Pagamento só pode ser registrado para OS com status FINALIZADA. Status atual: "
                + os.getStatus().getDescricao());
        }

        if (repository.existsByOrdemDeServicoId(osId)) {
            throw new BusinessException("Esta OS já possui um pagamento registrado.");
        }

        var pagamento = new Pagamento();
        pagamento.setOrdemDeServico(os);
        pagamento.setFormaPagamento(request.formaPagamento());
        pagamento.setValor(request.valor());

        return PagamentoResponse.from(repository.save(pagamento));
    }

    @Transactional(readOnly = true)
    public PagamentoResponse buscarPorOS(UUID osId) {
        return PagamentoResponse.from(
                repository.findByOrdemDeServicoId(osId)
                        .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado para a OS: " + osId))
        );
    }

    @Transactional
    public PagamentoResponse confirmar(UUID pagamentoId) {
        var pagamento = findOrThrow(pagamentoId);
        try {
            pagamento.confirmar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        var salvo = repository.save(pagamento);

        var os = pagamento.getOrdemDeServico();
        String emailCliente = os.getCliente().getEmail() != null
                ? os.getCliente().getEmail() : "sem-email@siase.com";

        emailService.enviarConfirmacaoPagamento(
                emailCliente,
                os.getCliente().getNome(),
                os.getNumero(),
                pagamento.getValor().toPlainString(),
                pagamento.getFormaPagamento().getDescricao()
        );

        // Avança OS para ENTREGUE após pagamento confirmado
        os.avancarStatus();
        osRepository.save(os);

        return PagamentoResponse.from(salvo);
    }

    @Transactional
    public PagamentoResponse cancelar(UUID pagamentoId) {
        var pagamento = findOrThrow(pagamentoId);
        try {
            pagamento.cancelar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        return PagamentoResponse.from(repository.save(pagamento));
    }

    private Pagamento findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado: " + id));
    }
}
