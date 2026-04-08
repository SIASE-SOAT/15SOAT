package br.com.fiap.siase.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        String nome,
        String tipoPessoa,
        String documento,
        String email,
        String telefone,
        String endereco,
        Boolean ativo,
        LocalDateTime criadoEm
) {}