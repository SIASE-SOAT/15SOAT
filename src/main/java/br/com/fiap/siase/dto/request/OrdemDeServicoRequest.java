package br.com.fiap.siase.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OrdemDeServicoRequest(
        @NotNull(message = "Cliente é obrigatório")
        UUID clienteId,
        @NotNull(message = "Veículo é obrigatório")
        UUID veiculoId,
        String observacoes,
        @NotEmpty(message = "A OS deve ter ao menos um serviço")
        @Valid
        List<ItemServicoRequest> itensServico,
        @Valid
        List<ItemPecaRequest> itensPeca
) {}
