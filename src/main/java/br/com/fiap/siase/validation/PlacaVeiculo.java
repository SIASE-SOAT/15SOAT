package br.com.fiap.siase.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PlacaVeiculoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)

public @interface PlacaVeiculo {
  String message() default "Placa inválida. Formatos aceitos: ABC1D23 (Mercosul) ou ABC-1234 (antigo)";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}