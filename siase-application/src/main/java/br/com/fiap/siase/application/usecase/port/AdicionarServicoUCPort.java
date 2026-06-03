package br.com.fiap.siase.application.usecase.port;

import br.com.fiap.siase.application.dto.input.ItemServicoRequest;
import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import java.util.UUID;

public interface AdicionarServicoUCPort {
    OrdemDeServicoResponse executar(UUID osId, ItemServicoRequest request);
}
