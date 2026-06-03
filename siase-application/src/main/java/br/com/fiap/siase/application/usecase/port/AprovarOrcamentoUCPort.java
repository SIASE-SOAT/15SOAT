package br.com.fiap.siase.application.usecase.port;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;

public interface AprovarOrcamentoUCPort {
    OrdemDeServicoResponse aprovar(String numero);
    OrdemDeServicoResponse recusar(String numero);
}
