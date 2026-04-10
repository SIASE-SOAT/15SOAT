package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.VeiculoRequest;
import br.com.fiap.siase.dto.response.VeiculoResponse;
import br.com.fiap.siase.service.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/veiculos")
@RequiredArgsConstructor
@Tag(name = "Veículos", description = "Gestão de veículos")
public class VeiculoController {

  private final VeiculoService veiculoService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Cadastrar novo veículo")
  public VeiculoResponse criar(@Valid @RequestBody VeiculoRequest request) {
    return veiculoService.criar(request);
  }

  @GetMapping
  @Operation(summary = "Listar todos os veículos")
  public List<VeiculoResponse> listarTodos() {
    return veiculoService.listarTodos();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Buscar veículo por ID")
  public VeiculoResponse buscarPorId(@PathVariable UUID id) {
    return veiculoService.buscarPorId(id);
  }

  @GetMapping("/placa/{placa}")
  @Operation(summary = "Buscar veículo por placa")
  public VeiculoResponse buscarPorPlaca(@PathVariable String placa) {
    return veiculoService.buscarPorPlaca(placa);
  }

  @GetMapping("/cliente/{clienteId}")
  @Operation(summary = "Listar veículos de um cliente")
  public List<VeiculoResponse> listarPorCliente(@PathVariable UUID clienteId) {
    return veiculoService.listarPorCliente(clienteId);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar veículo")
  public VeiculoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody VeiculoRequest request) {
    return veiculoService.atualizar(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Desativar veículo (soft delete)")
  public void desativar(@PathVariable UUID id) {
    veiculoService.desativar(id);
  }
}