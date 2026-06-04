package br.com.fiap.siase.application.usecase.port;

import br.com.fiap.siase.application.dto.output.PreparacaoAberturaOrdemResponse;

public interface PrepararAberturaOSUCPort {
    PreparacaoAberturaOrdemResponse executar(String documento, String placa);
}
