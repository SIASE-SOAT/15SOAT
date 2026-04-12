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
                var peca = pecaRepository.findById(req.pecaId())
                        .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + req.pecaId()));

                try {
                    peca.reservarEstoque(req.quantidade());
                } catch (IllegalStateException e) {
                    throw new BusinessException(e.getMessage());
                }
                pecaRepository.save(peca);

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

    @Transactional
    public OrdemDeServicoResponse avancarStatus(UUID id) {
        var os = findOrThrow(id);
        try {
            os.avancarStatus();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        return OrdemDeServicoResponse.from(repository.save(os));
    }

    private OrdemDeServico findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada: " + id));
    }

    private String gerarNumero() {
        String data = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sufixo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "OS-" + data + "-" + sufixo;
    }
}
