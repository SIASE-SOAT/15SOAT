package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.ClienteRequest;
import br.com.fiap.siase.dto.response.ClienteResponse;
import br.com.fiap.siase.service.ClienteService;
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
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestão de clientes da oficina (PF e PJ)")
public class ClienteController {

  private final ClienteService clienteService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Cadastrar novo cliente", description = "Cria um novo cliente PF ou PJ. O documento (CPF/CNPJ) deve ser único no sistema.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso"),
      @ApiResponse(responseCode = "400", description = "Dados inválidos"),
      @ApiResponse(responseCode = "401", description = "Não autenticado"),
      @ApiResponse(responseCode = "409", description = "Documento já cadastrado")
  })
  public ClienteResponse criar(@Valid @RequestBody ClienteRequest request) {
    return clienteService.criar(request);
  }

  @GetMapping
  @Operation(summary = "Listar todos os clientes", description = "Retorna todos os clientes cadastrados, incluindo inativos.")
  @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
  public List<ClienteResponse> listarTodos() {
    return clienteService.listarTodos();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Buscar cliente por ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
      @ApiResponse(responseCode = "401", description = "Não autenticado"),
      @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
  })
  public ClienteResponse buscarPorId(@Parameter(description = "ID do cliente") @PathVariable UUID id) {
    return clienteService.buscarPorId(id);
  }

  @GetMapping("/documento/{documento}")
  @Operation(summary = "Buscar cliente por CPF/CNPJ", description = "Aceita CPF/CNPJ com ou sem máscara de formatação.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
      @ApiResponse(responseCode = "401", description = "Não autenticado"),
      @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
  })
  public ClienteResponse buscarPorDocumento(@Parameter(description = "CPF ou CNPJ do cliente") @PathVariable String documento) {
    return clienteService.buscarPorDocumento(documento);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar dados do cliente")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso"),
      @ApiResponse(responseCode = "400", description = "Dados inválidos"),
      @ApiResponse(responseCode = "401", description = "Não autenticado"),
      @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
  })
  public ClienteResponse atualizar(
      @Parameter(description = "ID do cliente") @PathVariable UUID id,
      @Valid @RequestBody ClienteRequest request) {
    return clienteService.atualizar(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Desativar cliente (soft delete)", description = "Marca o cliente como inativo sem removê-lo do banco de dados.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Cliente desativado"),
      @ApiResponse(responseCode = "401", description = "Não autenticado"),
      @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
  })
  public void desativar(@Parameter(description = "ID do cliente") @PathVariable UUID id) {
    clienteService.desativar(id);
  }
}
