package br.com.fiap.siase.application.usecase.port;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import java.util.UUID;

public interface AvancarStatusUCPort {
    OrdemDeServicoResponse executar(UUID osId);
}
