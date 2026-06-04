package br.com.fiap.siase.application.usecase.port;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;

public interface AtualizarStatusViaWebhookUCPort {
    OrdemDeServicoResponse executar(String numero, String novoStatus, String tokenExterno);
}
