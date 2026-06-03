package br.com.fiap.siase.application.usecase.port;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.domain.enums.StatusOS;

import java.util.List;

public interface ListarOrdensServicoUCPort {
    List<OrdemDeServicoResponse> executar(StatusOS status);
}
