package br.com.fiap.siase.application.dto.input;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoRequest(
        @NotNull(message = "Cliente é obrigatório")
        UUID clienteId,
        @NotNull(message = "Veículo é obrigatório")
        UUID veiculoId,
        @NotNull(message = "Data e hora são obrigatórias")
        @Future(message = "O agendamento deve ser para uma data futura")
        LocalDateTime dataHora,
        String descricaoServicos
) {}
