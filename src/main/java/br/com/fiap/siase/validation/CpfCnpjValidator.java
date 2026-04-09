package br.com.fiap.siase.validation;

import br.com.caelum.stella.validation.CNPJValidator;
import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.InvalidStateException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfCnpjValidator implements ConstraintValidator<CpfCnpj, String> {

  @Override
  public boolean isValid(String valor, ConstraintValidatorContext context) {
    if (valor == null || valor.isBlank()) return false;

    String numeros = valor.replaceAll("[^0-9]", "");

    try {
      if (numeros.length() <= 11) {
        new CPFValidator().assertValid(numeros);
      } else {
        new CNPJValidator().assertValid(numeros);
      }
      return true;
    } catch (InvalidStateException e) {
      return false;
    }
  }
}