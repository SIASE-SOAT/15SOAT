package br.com.fiap.siase.application.usecase.port;

import br.com.fiap.siase.application.dto.input.ItemPecaRequest;
import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import java.util.UUID;

public interface AdicionarPecaUCPort {
    OrdemDeServicoResponse executar(UUID osId, ItemPecaRequest request);
}
