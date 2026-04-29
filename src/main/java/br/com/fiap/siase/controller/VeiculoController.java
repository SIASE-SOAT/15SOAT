package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.VeiculoRequest;
import br.com.fiap.siase.dto.response.VeiculoResponse;
import br.com.fiap.siase.service.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Veículos", description = "Gestão de veículos vinculados aos clientes da oficina")
public class VeiculoController {

  private final VeiculoService veiculoService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Cadastrar novo veículo", description = "Cria um novo veículo e vincula ao cliente informado. A placa deve ser única.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Veículo criado com sucesso"),
      @ApiResponse(responseCode = "400", description = "Dados inválidos ou placa fora do formato"),
      @ApiResponse(responseCode = "401", description = "Não autenticado"),
      @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
      @ApiResponse(responseCode = "409", description = "Placa já cadastrada")
  })
  public VeiculoResponse criar(@Valid @RequestBody VeiculoRequest request) {
    return veiculoService.criar(request);
  }

  @GetMapping
  @Operation(summary = "Listar todos os veículos", description = "Retorna todos os veículos cadastrados, incluindo inativos.")
  @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
  public List<VeiculoResponse> listarTodos() {
    return veiculoService.listarTodos();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Buscar veículo por ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Veículo encontrado"),
      @ApiResponse(responseCode = "401", description = "Não autenticado"),
      @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
  })
  public VeiculoResponse buscarPorId(@Parameter(description = "ID do veículo") @PathVariable UUID id) {
    return veiculoService.buscarPorId(id);
  }

  @GetMapping("/placa/{placa}")
  @Operation(summary = "Buscar veículo por placa", description = "Aceita placa no formato antigo (AAA-0000) ou Mercosul (AAA0A00).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Veículo encontrado"),
      @ApiResponse(responseCode = "401", description = "Não autenticado"),
      @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
  })
  public VeiculoResponse buscarPorPlaca(@Parameter(description = "Placa do veículo") @PathVariable String placa) {
    return veiculoService.buscarPorPlaca(placa);
  }

  @GetMapping("/cliente/{clienteId}")
  @Operation(summary = "Listar veículos de um cliente", description = "Retorna todos os veículos ativos vinculados ao cliente informado.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
      @ApiResponse(responseCode = "401", description = "Não autenticado"),
      @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
  })
  public List<VeiculoResponse> listarPorCliente(@Parameter(description = "ID do cliente") @PathVariable UUID clienteId) {
    return veiculoService.listarPorCliente(clienteId);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar veículo")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Veículo atualizado"),
      @ApiResponse(responseCode = "400", description = "Dados inválidos"),
      @ApiResponse(responseCode = "401", description = "Não autenticado"),
      @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
  })
  public VeiculoResponse atualizar(
      @Parameter(description = "ID do veículo") @PathVariable UUID id,
      @Valid @RequestBody VeiculoRequest request) {
    return veiculoService.atualizar(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Desativar veículo (soft delete)", description = "Marca o veículo como inativo sem removê-lo do banco de dados.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Veículo desativado"),
      @ApiResponse(responseCode = "401", description = "Não autenticado"),
      @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
  })
  public void desativar(@Parameter(description = "ID do veículo") @PathVariable UUID id) {
    veiculoService.desativar(id);
  }
}
