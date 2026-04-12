package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.AgendamentoRequest;
import br.com.fiap.siase.dto.response.AgendamentoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Agendamento;
import br.com.fiap.siase.model.enums.StatusAgendamento;
import br.com.fiap.siase.repository.AgendamentoRepository;
import br.com.fiap.siase.repository.ClienteRepository;
import br.com.fiap.siase.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository repository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final EmailService emailService;

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        var cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.clienteId()));

        var veiculo = veiculoRepository.findById(request.veiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado: " + request.veiculoId()));

        if (!veiculo.getCliente().getId().equals(cliente.getId())) {
            throw new BusinessException("O veículo informado não pertence ao cliente.");
        }

        var agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setVeiculo(veiculo);
        agendamento.setDataHora(request.dataHora());
        agendamento.setDescricaoServicos(request.descricaoServicos());

        var salvo = repository.save(agendamento);

        String emailCliente = cliente.getEmail() != null ? cliente.getEmail() : "sem-email@siase.com";
        emailService.enviarConfirmacaoAgendamento(
                emailCliente,
                cliente.getNome(),
                request.dataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                veiculo.getMarca() + " " + veiculo.getModelo() + " - " + veiculo.getPlaca()
        );

        return AgendamentoResponse.from(salvo);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar() {
        return repository.findAll().stream().map(AgendamentoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listarPorStatus(StatusAgendamento status) {
        return repository.findByStatus(status).stream().map(AgendamentoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listarPorCliente(UUID clienteId) {
        return repository.findByClienteId(clienteId).stream().map(AgendamentoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscarPorId(UUID id) {
        return AgendamentoResponse.from(findOrThrow(id));
    }

    @Transactional
    public AgendamentoResponse confirmar(UUID id) {
        var agendamento = findOrThrow(id);
        try {
            agendamento.confirmar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        return AgendamentoResponse.from(repository.save(agendamento));
    }

    @Transactional
    public AgendamentoResponse cancelar(UUID id) {
        var agendamento = findOrThrow(id);
        try {
            agendamento.cancelar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        return AgendamentoResponse.from(repository.save(agendamento));
    }

    private Agendamento findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado: " + id));
    }
}
