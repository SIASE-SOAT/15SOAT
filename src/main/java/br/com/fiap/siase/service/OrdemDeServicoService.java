package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.ItemPecaRequest;
import br.com.fiap.siase.dto.request.ItemServicoRequest;
import br.com.fiap.siase.dto.request.OrdemDeServicoRequest;
import br.com.fiap.siase.dto.response.OrdemDeServicoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.ItemPeca;
import br.com.fiap.siase.model.ItemServico;
import br.com.fiap.siase.model.OrdemDeServico;
import br.com.fiap.siase.model.enums.StatusOS;
import br.com.fiap.siase.repository.ClienteRepository;
import br.com.fiap.siase.repository.OrdemDeServicoRepository;
import br.com.fiap.siase.repository.PecaRepository;
import br.com.fiap.siase.repository.ServicoRepository;
import br.com.fiap.siase.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrdemDeServicoService {

    private final OrdemDeServicoRepository repository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final ServicoRepository servicoRepository;
    private final PecaRepository pecaRepository;
    private final EmailService emailService;

    @Transactional
    public OrdemDeServicoResponse criar(OrdemDeServicoRequest request) {
        var cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.clienteId()));

        var veiculo = veiculoRepository.findById(request.veiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado: " + request.veiculoId()));

        if (!veiculo.getCliente().getId().equals(cliente.getId())) {
            throw new BusinessException("O veículo informado não pertence ao cliente.");
        }

        var os = new OrdemDeServico();
        os.setNumero(gerarNumero());
        os.setCliente(cliente);
        os.setVeiculo(veiculo);
        os.setObservacoes(request.observacoes());

        for (ItemServicoRequest req : request.itensServico()) {
            var servico = servicoRepository.findById(req.servicoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + req.servicoId()));

            var item = new ItemServico();
            item.setOrdemDeServico(os);
            item.setServico(servico);
            item.setPrecoUnitario(servico.getPreco());
            item.setTempoEstimadoMinutos(servico.getTempoEstimadoMinutos());
            item.setObservacoes(req.observacoes());
            os.getItensServico().add(item);
        }

        if (request.itensPeca() != null) {
            for (ItemPecaRequest req : request.itensPeca()) {
                var peca = pecaRepository.findByIdParaAtualizacao(req.pecaId())
                        .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + req.pecaId()));

                try {
                    peca.reservarEstoque(req.quantidade());
                } catch (IllegalStateException e) {
                    throw new BusinessException(e.getMessage());
                }

                var item = new ItemPeca();
                item.setOrdemDeServico(os);
                item.setPeca(peca);
                item.setQuantidade(req.quantidade());
                item.setPrecoUnitario(peca.getPreco());
                os.getItensPeca().add(item);
            }
        }

        os.recalcularTotais();
        return OrdemDeServicoResponse.from(repository.save(os));
    }

    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponse> listar() {
        return repository.findAll().stream()
                .map(OrdemDeServicoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponse> listarPorStatus(StatusOS status) {
        return repository.findByStatus(status).stream()
                .map(OrdemDeServicoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdemDeServicoResponse buscarPorId(UUID id) {
        return OrdemDeServicoResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public OrdemDeServicoResponse buscarPorNumero(String numero) {
        return OrdemDeServicoResponse.from(
                repository.findByNumero(numero)
                        .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + numero))
        );
    }

    @Transactional(readOnly = true)
    public double calcularTempoMedioExecucaoMinutos() {
        Double resultado = repository.calcularTempoMedioExecucaoMinutos();
        return resultado != null ? resultado : 0.0;
    }

    @Transactional
    public OrdemDeServicoResponse avancarStatus(UUID id) {
        var os = findOrThrow(id);
        StatusOS statusAnterior = os.getStatus();

        try {
            os.avancarStatus();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        var salvo = repository.save(os);

        // Dispara email quando orçamento é enviado para aprovação
        if (os.getStatus() == StatusOS.AGUARDANDO_APROVACAO) {
            String email = os.getCliente().getEmail() != null
                    ? os.getCliente().getEmail() : "sem-email@siase.com";
            emailService.enviarOrcamentoParaAprovacao(
                    email,
                    os.getCliente().getNome(),
                    os.getNumero(),
                    os.getTotal().toPlainString()
            );
        }

        // Dispara email quando orçamento é aprovado e execução inicia
        if (statusAnterior == StatusOS.AGUARDANDO_APROVACAO && os.getStatus() == StatusOS.EM_EXECUCAO) {
            String email = os.getCliente().getEmail() != null
                    ? os.getCliente().getEmail() : "sem-email@siase.com";
            emailService.enviarOrcamentoAprovado(email, os.getCliente().getNome(), os.getNumero());
        }

        return OrdemDeServicoResponse.from(salvo);
    }

    @Transactional
    public OrdemDeServicoResponse adicionarPecaAOrdem(UUID orderId, ItemPecaRequest request) {
        var os = findOrThrow(orderId);

        if (!isStatusPermitidoParaAdicionarPeca(os.getStatus())) {
            throw new BusinessException("Não é possível adicionar peças em uma ordem com status " + os.getStatus().getDescricao());
        }

        if (jaTemPeca(os, request.pecaId())) {
            throw new BusinessException("Esta peça já foi adicionada a esta ordem de serviço.");
        }

        var peca = pecaRepository.findByIdParaAtualizacao(request.pecaId())
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + request.pecaId()));

        if (!Boolean.TRUE.equals(peca.getAtivo())) {
            throw new BusinessException("Peça desativada não pode ser adicionada: " + request.pecaId());
        }

        try {
            peca.reservarEstoque(request.quantidade());
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }


        var item = new ItemPeca();
        item.setOrdemDeServico(os);
        item.setPeca(peca);
        item.setQuantidade(request.quantidade());
        item.setPrecoUnitario(peca.getPreco());
        os.getItensPeca().add(item);

        os.recalcularTotais();

        return OrdemDeServicoResponse.from(repository.save(os));
    }

    @Transactional
    public OrdemDeServicoResponse adicionarServicoAOrdem(UUID orderId, ItemServicoRequest request) {
        var os = findOrThrow(orderId);

        if (!isStatusPermitidoParaAdicionarServico(os.getStatus())) {
            throw new BusinessException("Não é possível adicionar serviços em uma ordem com status " + os.getStatus().getDescricao());
        }

        if (jaTemServico(os, request.servicoId())) {
            throw new BusinessException("Este serviço já foi adicionado a esta ordem de serviço.");
        }

        var servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + request.servicoId()));

        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new BusinessException("Serviço desativado não pode ser adicionado: " + request.servicoId());
        }

        var item = new ItemServico();
        item.setOrdemDeServico(os);
        item.setServico(servico);
        item.setPrecoUnitario(servico.getPreco());
        item.setTempoEstimadoMinutos(servico.getTempoEstimadoMinutos());
        item.setObservacoes(request.observacoes());
        os.getItensServico().add(item);

        os.recalcularTotais();

        return OrdemDeServicoResponse.from(repository.save(os));
    }

    @Transactional
    public OrdemDeServicoResponse cancelar(UUID id) {
        var os = findOrThrow(id);
        try {
            os.cancelar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        // Devolve o estoque de todas as peças da OS
        for (var itemPeca : os.getItensPeca()) {
            pecaRepository.findByIdParaAtualizacao(itemPeca.getPeca().getId())
                    .ifPresent(p -> p.devolverEstoque(itemPeca.getQuantidade()));
        }

        var salvo = repository.save(os);

        String email = os.getCliente().getEmail() != null
                ? os.getCliente().getEmail() : "sem-email@siase.com";
        emailService.enviarOrcamentoCancelado(email, os.getCliente().getNome(), os.getNumero());

        return OrdemDeServicoResponse.from(salvo);
    }

    private OrdemDeServico findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + id));
    }

    private boolean jaTemPeca(OrdemDeServico os, UUID pecaId) {
        return os.getItensPeca().stream()
                .anyMatch(item -> item.getPeca().getId().equals(pecaId));
    }

    private boolean jaTemServico(OrdemDeServico os, UUID servicoId) {
        return os.getItensServico().stream()
                .anyMatch(item -> item.getServico().getId().equals(servicoId));
    }

    private boolean isStatusPermitidoParaAdicionarPeca(StatusOS status) {
        return status == StatusOS.RECEBIDA
                || status == StatusOS.EM_DIAGNOSTICO
                || status == StatusOS.AGUARDANDO_APROVACAO
                || status == StatusOS.EM_EXECUCAO;
    }

    private boolean isStatusPermitidoParaAdicionarServico(StatusOS status) {
        return status == StatusOS.RECEBIDA
                || status == StatusOS.EM_DIAGNOSTICO
                || status == StatusOS.AGUARDANDO_APROVACAO
                || status == StatusOS.EM_EXECUCAO;
    }

    private String gerarNumero() {
        String data = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sufixo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "OS-" + data + "-" + sufixo;
    }
}
