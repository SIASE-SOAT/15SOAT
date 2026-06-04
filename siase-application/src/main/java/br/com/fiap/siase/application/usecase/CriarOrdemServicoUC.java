package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.input.ItemPecaRequest;
import br.com.fiap.siase.application.dto.input.ItemServicoRequest;
import br.com.fiap.siase.application.dto.input.OrdemDeServicoRequest;
import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.ItemPeca;
import br.com.fiap.siase.domain.model.ItemServico;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
import br.com.fiap.siase.domain.enums.StatusOS;

import br.com.fiap.siase.application.usecase.port.CriarOrdemServicoUCPort;

import java.util.List;

public class CriarOrdemServicoUC implements CriarOrdemServicoUCPort {

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final ClienteRepositoryPort clienteRepository;
    private final VeiculoRepositoryPort veiculoRepository;
    private final ServicoRepositoryPort servicoRepository;
    private final PecaRepositoryPort pecaRepository;

    public CriarOrdemServicoUC(
            OrdemServicoRepositoryPort ordemServicoRepository,
            ClienteRepositoryPort clienteRepository,
            VeiculoRepositoryPort veiculoRepository,
            ServicoRepositoryPort servicoRepository,
            PecaRepositoryPort pecaRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.servicoRepository = servicoRepository;
        this.pecaRepository = pecaRepository;
    }

    public OrdemDeServicoResponse executar(OrdemDeServicoRequest request) {
        var cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.clienteId()));

        var veiculo = veiculoRepository.findById(request.veiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado: " + request.veiculoId()));

        if (!veiculo.getCliente().getId().equals(cliente.getId())) {
            throw new BusinessException("O veículo informado não pertence ao cliente.");
        }

        if (ordemServicoRepository.existsByVeiculoIdAndStatusNotIn(
                veiculo.getId(), List.of(StatusOS.ENTREGUE, StatusOS.CANCELADA))) {
            throw new BusinessException(
                    "O veículo já possui uma ordem de serviço em andamento. " +
                            "Finalize ou cancele a OS existente antes de abrir uma nova.");
        }

        var os = new OrdemDeServico();
        os.setNumero(OrdemDeServico.gerarNumero());
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
        return OrdemDeServicoResponse.from(ordemServicoRepository.save(os));
    }

}
