package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.application.dto.output.OrdemDeServicoResponse;
import br.com.fiap.siase.application.usecase.port.ListarOrdensDoClienteUCPort;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;

import java.util.Comparator;
import java.util.List;

public class ListarOrdensDoClienteUC implements ListarOrdensDoClienteUCPort {

  private final OrdemServicoRepositoryPort ordemServicoRepository;
  private final ClienteRepositoryPort clienteRepository;

  public ListarOrdensDoClienteUC(
          OrdemServicoRepositoryPort ordemServicoRepository,
          ClienteRepositoryPort clienteRepository) {
    this.ordemServicoRepository = ordemServicoRepository;
    this.clienteRepository = clienteRepository;
  }

  @Override
  public List<OrdemDeServicoResponse> executarPorDocumento(String documento) {
    var documentoNormalizado = documento == null ? "" : documento.replaceAll("\\D", "");
    if (documentoNormalizado.isEmpty()) {
      throw new ResourceNotFoundException("Documento autenticado inválido");
    }

    var cliente = clienteRepository.findByDocumento(documentoNormalizado)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Cliente não encontrado para o documento autenticado"));

    return ordemServicoRepository.findByClienteId(cliente.getId()).stream()
            .sorted(Comparator.comparing(
                    ordem -> ordem.getDataAbertura(),
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .map(OrdemDeServicoResponse::from)
            .toList();
  }
}
