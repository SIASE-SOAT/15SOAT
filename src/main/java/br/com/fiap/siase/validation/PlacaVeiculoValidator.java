package br.com.fiap.siase.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PlacaVeiculoValidator implements ConstraintValidator<PlacaVeiculo, String> {

  private static final Pattern MERCOSUL = Pattern.compile("^[A-Z]{3}[0-9][A-Z][0-9]{2}$");
  private static final Pattern ANTIGO = Pattern.compile("^[A-Z]{3}-?[0-9]{4}$");

  @Override
  public boolean isValid(String valor, ConstraintValidatorContext context) {
    if (valor == null || valor.isBlank()) return false;
    String upper = valor.toUpperCase().trim();
    return MERCOSUL.matcher(upper).matches() || ANTIGO.matcher(upper).matches();
  }
}