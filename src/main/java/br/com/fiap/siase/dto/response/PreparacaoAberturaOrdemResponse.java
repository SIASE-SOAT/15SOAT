package br.com.fiap.siase.dto.response;

import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.Veiculo;

import java.util.List;
import java.util.UUID;

public record PreparacaoAberturaOrdemResponse(
        ClienteIdentificadoResponse cliente,
        List<VeiculoIdentificadoResponse> veiculos,
        VeiculoIdentificadoResponse veiculoSelecionado,
        boolean prontoParaAbertura
) {

    public record ClienteIdentificadoResponse(
            UUID id,
            String nome,
            String documento,
            String email,
            String telefone
    ) {
        public static ClienteIdentificadoResponse from(Cliente cliente) {
            return new ClienteIdentificadoResponse(
                    cliente.getId(),
                    cliente.getNome(),
                    cliente.getDocumento(),
                    cliente.getEmail(),
                    cliente.getTelefone()
            );
        }
    }

    public record VeiculoIdentificadoResponse(
            UUID id,
            String placa,
            String marca,
            String modelo,
            Integer ano,
            boolean ativo
    ) {
        public static VeiculoIdentificadoResponse from(Veiculo veiculo) {
            return new VeiculoIdentificadoResponse(
                    veiculo.getId(),
                    veiculo.getPlaca(),
                    veiculo.getMarca(),
                    veiculo.getModelo(),
                    veiculo.getAno(),
                    Boolean.TRUE.equals(veiculo.getAtivo())
            );
        }
    }
}
