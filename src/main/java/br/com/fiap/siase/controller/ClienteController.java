package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.ClienteRequest;
import br.com.fiap.siase.dto.response.ClienteResponse;
import br.com.fiap.siase.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestão de clientes da oficina")
public class ClienteController {

  private final ClienteService clienteService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Cadastrar novo cliente")
  public ClienteResponse criar(@Valid @RequestBody ClienteRequest request) {
    return clienteService.criar(request);
  }

  @GetMapping
  @Operation(summary = "Listar todos os clientes")
  public List<ClienteResponse> listarTodos() {
    return clienteService.listarTodos();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Buscar cliente por ID")
  public ClienteResponse buscarPorId(@PathVariable UUID id) {
    return clienteService.buscarPorId(id);
  }

  @GetMapping("/documento/{documento}")
  @Operation(summary = "Buscar cliente por CPF/CNPJ")
  public ClienteResponse buscarPorDocumento(@PathVariable String documento) {
    return clienteService.buscarPorDocumento(documento);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar dados do cliente")
  public ClienteResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ClienteRequest request) {
    return clienteService.atualizar(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Desativar cliente (soft delete)")
  public void desativar(@PathVariable UUID id) {
    clienteService.desativar(id);
  }
}