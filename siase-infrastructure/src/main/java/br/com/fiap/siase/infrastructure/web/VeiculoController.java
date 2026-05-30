package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.VeiculoRequest;
import br.com.fiap.siase.application.dto.output.VeiculoResponse;
import br.com.fiap.siase.domain.exception.DuplicateResourceException;
import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
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
@RequestMapping("/veiculos")
@Tag(name = "Veículos", description = "Gestão de veículos vinculados aos clientes da oficina")
public class VeiculoController {

    private final VeiculoRepositoryPort veiculoRepository;
    private final ClienteRepositoryPort clienteRepository;

    public VeiculoController(VeiculoRepositoryPort veiculoRepository,
                             ClienteRepositoryPort clienteRepository) {
        this.veiculoRepository = veiculoRepository;
        this.clienteRepository = clienteRepository;
    }

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
    @Transactional
    public VeiculoResponse criar(@Valid @RequestBody VeiculoRequest request) {
        String placaUpper = request.placa().toUpperCase().trim();

        if (veiculoRepository.findByPlaca(placaUpper).isPresent()) {
            throw new DuplicateResourceException("Já existe um veículo com a placa: " + placaUpper);
        }

        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.clienteId()));

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(placaUpper);
        veiculo.setMarca(request.marca());
        veiculo.setModelo(request.modelo());
        veiculo.setAno(request.ano());
        veiculo.setCor(request.cor());
        veiculo.setCliente(cliente);

        return toResponse(veiculoRepository.save(veiculo));
    }

    @GetMapping
    @Operation(summary = "Listar todos os veículos", description = "Retorna todos os veículos cadastrados, incluindo inativos.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @Transactional(readOnly = true)
    public List<VeiculoResponse> listarTodos() {
        return veiculoRepository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veículo por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Veículo encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    })
    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorId(@Parameter(description = "ID do veículo") @PathVariable UUID id) {
        return toResponse(findById(id));
    }

    @GetMapping("/placa/{placa}")
    @Operation(summary = "Buscar veículo por placa", description = "Aceita placa no formato antigo (AAA-0000) ou Mercosul (AAA0A00).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Veículo encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    })
    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorPlaca(@Parameter(description = "Placa do veículo") @PathVariable String placa) {
        return veiculoRepository.findByPlaca(placa.toUpperCase().trim())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado: " + placa));
    }

    @GetMapping("/cliente/{clienteId}")
    @Operation(summary = "Listar veículos de um cliente", description = "Retorna todos os veículos ativos vinculados ao cliente informado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    @Transactional(readOnly = true)
    public List<VeiculoResponse> listarPorCliente(@Parameter(description = "ID do cliente") @PathVariable UUID clienteId) {
        return veiculoRepository.findByClienteId(clienteId).stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar veículo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Veículo atualizado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    })
    @Transactional
    public VeiculoResponse atualizar(
            @Parameter(description = "ID do veículo") @PathVariable UUID id,
            @Valid @RequestBody VeiculoRequest request) {
        Veiculo veiculo = findById(id);
        veiculo.setPlaca(request.placa().toUpperCase().trim());
        veiculo.setMarca(request.marca());
        veiculo.setModelo(request.modelo());
        veiculo.setAno(request.ano());
        veiculo.setCor(request.cor());
        return toResponse(veiculoRepository.save(veiculo));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativar veículo (soft delete)", description = "Marca o veículo como inativo sem removê-lo do banco de dados.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Veículo desativado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    })
    @Transactional
    public void desativar(@Parameter(description = "ID do veículo") @PathVariable UUID id) {
        Veiculo veiculo = findById(id);
        veiculo.setAtivo(false);
        veiculoRepository.save(veiculo);
    }

    private Veiculo findById(UUID id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado: " + id));
    }

    private VeiculoResponse toResponse(Veiculo v) {
        return new VeiculoResponse(
                v.getId(), v.getPlaca(), v.getMarca(), v.getModelo(), v.getAno(),
                v.getCor(), v.getAtivo(), v.getCliente().getId(), v.getCliente().getNome(),
                v.getCriadoEm()
        );
    }
}
