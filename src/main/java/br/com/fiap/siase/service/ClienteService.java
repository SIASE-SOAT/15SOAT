package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.ClienteRequest;
import br.com.fiap.siase.dto.response.ClienteResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.DuplicateResourceException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteService {

  private final ClienteRepository clienteRepository;

  public ClienteResponse criar(ClienteRequest request) {
    String docLimpo = limparDocumento(request.documento());

    if (clienteRepository.existsByDocumento(docLimpo)) {
      throw new DuplicateResourceException("Já existe um cliente com o documento: " + request.documento());
    }

    Cliente cliente = new Cliente();
    cliente.setNome(request.nome());
    cliente.setTipoPessoa(request.tipoPessoa());
    cliente.setDocumento(docLimpo);
    cliente.setEmail(request.email());
    cliente.setTelefone(request.telefone());
    cliente.setEndereco(request.endereco());

    return toResponse(clienteRepository.save(cliente));
  }

  @Transactional(readOnly = true)
  public ClienteResponse buscarPorId(UUID id) {
    return toResponse(findById(id));
  }

  @Transactional(readOnly = true)
  public ClienteResponse buscarPorDocumento(String documento) {
    String docLimpo = limparDocumento(documento);
    return clienteRepository.findByDocumento(docLimpo)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente", documento));
  }

  @Transactional(readOnly = true)
  public List<ClienteResponse> listarTodos() {
    return clienteRepository.findAll().stream().map(this::toResponse).toList();
  }

  public ClienteResponse atualizar(UUID id, ClienteRequest request) {
    Cliente cliente = findById(id);
    cliente.setNome(request.nome());
    cliente.setTipoPessoa(request.tipoPessoa());
    cliente.setEmail(request.email());
    cliente.setTelefone(request.telefone());
    cliente.setEndereco(request.endereco());
    return toResponse(clienteRepository.save(cliente));
  }

  public void desativar(UUID id) {
    Cliente cliente = findById(id);
    cliente.setAtivo(false);
    clienteRepository.save(cliente);
  }

  public Cliente findById(UUID id) {
    return clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
  }

  private String limparDocumento(String documento) {
    return documento.replaceAll("[^0-9]", "");
  }

  private ClienteResponse toResponse(Cliente c) {
    return new ClienteResponse(
            c.getId(), c.getNome(), c.getTipoPessoa().name(), c.getDocumento(),
            c.getEmail(), c.getTelefone(), c.getEndereco(), c.getAtivo(), c.getCriadoEm()
    );
  }
}