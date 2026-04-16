package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.VeiculoRequest;
import br.com.fiap.siase.dto.response.VeiculoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.DuplicateResourceException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.Veiculo;
import br.com.fiap.siase.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VeiculoService {

  private final VeiculoRepository veiculoRepository;
  private final ClienteService clienteService;

  public VeiculoResponse criar(VeiculoRequest request) {
    String placaUpper = request.placa().toUpperCase().trim();

    if (veiculoRepository.existsByPlaca(placaUpper)) {
      throw new DuplicateResourceException("Já existe um veículo com a placa: " + placaUpper);
    }

    Cliente cliente = clienteService.findById(request.clienteId());

    Veiculo veiculo = new Veiculo();
    veiculo.setPlaca(placaUpper);
    veiculo.setMarca(request.marca());
    veiculo.setModelo(request.modelo());
    veiculo.setAno(request.ano());
    veiculo.setCor(request.cor());
    veiculo.setCliente(cliente);

    return toResponse(veiculoRepository.save(veiculo));
  }

  @Transactional(readOnly = true)
  public VeiculoResponse buscarPorId(UUID id) {
    return toResponse(findById(id));
  }

  @Transactional(readOnly = true)
  public VeiculoResponse buscarPorPlaca(String placa) {
    return veiculoRepository.findByPlaca(placa.toUpperCase().trim())
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Veículo", placa));
  }

  @Transactional(readOnly = true)
  public List<VeiculoResponse> listarTodos() {
    return veiculoRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<VeiculoResponse> listarPorCliente(UUID clienteId) {
    return veiculoRepository.findByClienteId(clienteId).stream().map(this::toResponse).toList();
  }

  public VeiculoResponse atualizar(UUID id, VeiculoRequest request) {
    Veiculo veiculo = findById(id);
    veiculo.setPlaca(request.placa().toUpperCase().trim());
    veiculo.setMarca(request.marca());
    veiculo.setModelo(request.modelo());
    veiculo.setAno(request.ano());
    veiculo.setCor(request.cor());
    return toResponse(veiculoRepository.save(veiculo));
  }

  public void desativar(UUID id) {
    Veiculo veiculo = findById(id);
    veiculo.setAtivo(false);
    veiculoRepository.save(veiculo);
  }

  public Veiculo findById(UUID id) {
    return veiculoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Veículo", id));
  }

  private VeiculoResponse toResponse(Veiculo v) {
    return new VeiculoResponse(
            v.getId(), v.getPlaca(), v.getMarca(), v.getModelo(), v.getAno(),
            v.getCor(), v.getAtivo(), v.getCliente().getId(), v.getCliente().getNome(),
            v.getCriadoEm()
    );
  }
}