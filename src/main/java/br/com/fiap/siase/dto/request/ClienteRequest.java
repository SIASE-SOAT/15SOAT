package br.com.fiap.siase.dto.request;

import br.com.fiap.siase.model.enums.TipoPessoa;
import br.com.fiap.siase.validation.CpfCnpj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClienteRequest(
        @NotBlank(message = "Nome é obrigatorio")
        String nome,

        @NotNull(message = "Tipo pessoa é obrigatorio")
        TipoPessoa tipoPessoa,

        @NotBlank(message = "Documento é obrigatorio")
        @CpfCnpj
        String documento,

        @Email(message = "Email invalido")
        String email,

        String telefone,

        String endereco
)
{}
