package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.ClienteRequest;
import br.com.fiap.siase.application.dto.output.ClienteResponse;
import br.com.fiap.siase.domain.exception.DuplicateResourceException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Gestão de clientes da oficina (PF e PJ)")
public class ClienteController {

    private final ClienteRepositoryPort clienteRepository;

    public ClienteController(ClienteRepositoryPort clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar novo cliente", description = "Cria um novo cliente PF ou PJ. O documento (CPF/CNPJ) deve ser único no sistema.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "409", description = "Documento já cadastrado")
    })
    @Transactional
    public ClienteResponse criar(@Valid @RequestBody ClienteRequest request) {
        String docLimpo = limparDocumento(request.documento());

        if (clienteRepository.findByDocumento(docLimpo).isPresent()) {
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

    @GetMapping
    @Operation(summary = "Listar todos os clientes", description = "Retorna todos os clientes cadastrados, incluindo inativos.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(@Parameter(description = "ID do cliente") @PathVariable UUID id) {
        return toResponse(findById(id));
    }

    @GetMapping("/documento/{documento}")
    @Operation(summary = "Buscar cliente por CPF/CNPJ", description = "Aceita CPF/CNPJ com ou sem máscara de formatação.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    @Transactional(readOnly = true)
    public ClienteResponse buscarPorDocumento(@Parameter(description = "CPF ou CNPJ do cliente") @PathVariable String documento) {
        String docLimpo = limparDocumento(documento);
        return clienteRepository.findByDocumento(docLimpo)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + documento));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do cliente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    @Transactional
    public ClienteResponse atualizar(
            @Parameter(description = "ID do cliente") @PathVariable UUID id,
            @Valid @RequestBody ClienteRequest request) {
        Cliente cliente = findById(id);
        cliente.setNome(request.nome());
        cliente.setTipoPessoa(request.tipoPessoa());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        cliente.setEndereco(request.endereco());
        return toResponse(clienteRepository.save(cliente));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativar cliente (soft delete)", description = "Marca o cliente como inativo sem removê-lo do banco de dados.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Cliente desativado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    @Transactional
    public void desativar(@Parameter(description = "ID do cliente") @PathVariable UUID id) {
        Cliente cliente = findById(id);
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    private Cliente findById(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + id));
    }

    private String limparDocumento(String documento) {
        return documento.replaceAll("\\D", "");
    }

    private ClienteResponse toResponse(Cliente c) {
        return new ClienteResponse(
                c.getId(), c.getNome(), c.getTipoPessoa().name(), c.getDocumento(),
                c.getEmail(), c.getTelefone(), c.getEndereco(), c.getAtivo(), c.getCriadoEm()
        );
    }
}
