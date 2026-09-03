package br.com.fiap.siase.application.usecase.port;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;

import java.util.List;

public interface ListarOrdensDoClienteUCPort {
  List<OrdemDeServicoResponse> executarPorDocumento(String documento);
}
