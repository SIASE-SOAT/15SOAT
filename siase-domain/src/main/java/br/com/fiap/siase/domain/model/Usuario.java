package br.com.fiap.siase.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {
    private UUID id;
    private String username;
    private String password;
    @Builder.Default
    private Boolean ativo = true;
    private LocalDateTime criadoEm;
}
