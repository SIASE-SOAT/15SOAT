package br.com.fiap.siase.application.dto.input;

import jakarta.validation.constraints.NotBlank;

public record AtualizarStatusWebhookRequest(
        @NotBlank(message = "Número da OS é obrigatório")
        String numero,
        @NotBlank(message = "Novo status é obrigatório")
        String novoStatus,
        @NotBlank(message = "Token de serviço externo é obrigatório")
        String tokenExterno
) {}
