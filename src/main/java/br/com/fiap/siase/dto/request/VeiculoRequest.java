package br.com.fiap.siase.dto.request;

import br.com.fiap.siase.validation.PlacaVeiculo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VeiculoRequest(
        @NotBlank(message = "Placa é obrigatória")
        @PlacaVeiculo
        String placa,

        @NotBlank(message = "Marca é obrigatória")
        String marca,

        @NotBlank(message = "Modelo é obrigatório")
        String modelo,

        @NotNull(message = "Ano é obrigatório")
        @Min(value = 1900, message = "Ano inválido")
        @Max(value = 2027, message = "Ano inválido")
        Integer ano,

        String cor,

        @NotNull(message = "ID do cliente é obrigatório")
        UUID clienteId
) {}