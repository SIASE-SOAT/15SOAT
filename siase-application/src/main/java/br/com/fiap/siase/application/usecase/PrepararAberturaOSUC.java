package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.PreparacaoAberturaOrdemResponse;
import br.com.fiap.siase.application.usecase.port.PrepararAberturaOSUCPort;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;

import java.util.List;

public class PrepararAberturaOSUC implements PrepararAberturaOSUCPort {

    private final ClienteRepositoryPort clienteRepository;
    private final VeiculoRepositoryPort veiculoRepository;

    public PrepararAberturaOSUC(ClienteRepositoryPort clienteRepository, VeiculoRepositoryPort veiculoRepository) {
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public PreparacaoAberturaOrdemResponse executar(String documento, String placa) {
        String documentoLimpo = documento.replaceAll("\\D", "");

        var cliente = clienteRepository.findByDocumento(documentoLimpo)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o documento: " + documento));

        List<PreparacaoAberturaOrdemResponse.VeiculoIdentificadoResponse> veiculosAtivos =
                veiculoRepository.findByClienteId(cliente.getId()).stream()
                        .filter(v -> Boolean.TRUE.equals(v.getAtivo()))
                        .map(PreparacaoAberturaOrdemResponse.VeiculoIdentificadoResponse::from)
                        .toList();

        PreparacaoAberturaOrdemResponse.VeiculoIdentificadoResponse veiculoSelecionado = null;
        if (placa != null && !placa.isBlank()) {
            String placaNormalizada = placa.toUpperCase().trim();
            var veiculo = veiculoRepository.findByPlaca(placaNormalizada)
                    .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado para a placa: " + placa));

            if (!veiculo.getCliente().getId().equals(cliente.getId())) {
                throw new BusinessException("O veículo informado não pertence ao cliente identificado pelo documento.");
            }

            if (!Boolean.TRUE.equals(veiculo.getAtivo())) {
                throw new BusinessException("O veículo informado está inativo e não pode ser usado para abrir uma OS.");
            }

            veiculoSelecionado = PreparacaoAberturaOrdemResponse.VeiculoIdentificadoResponse.from(veiculo);
        }

        return new PreparacaoAberturaOrdemResponse(
                PreparacaoAberturaOrdemResponse.ClienteIdentificadoResponse.from(cliente),
                veiculosAtivos,
                veiculoSelecionado,
                veiculoSelecionado != null
        );
    }
}
