package br.com.fiap.siase.application.usecase.port;

import br.com.fiap.siase.application.dto.input.OrdemDeServicoRequest;
import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;

public interface CriarOrdemServicoUCPort {
    OrdemDeServicoResponse executar(OrdemDeServicoRequest request);
}
