package br.com.fiap.siase.domain.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CpfCnpjValidator: validacao de CPF e CNPJ")
class CpfCnpjValidatorTest {

    private CpfCnpjValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CpfCnpjValidator();
    }

    @Test
    @DisplayName("Deve aceitar CPF valido sem formatacao")
    void deveAceitarCpfValidoSemFormatacao() {
        assertThat(validator.isValid("52998224725", null)).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar CPF valido com formatacao")
    void deveAceitarCpfValidoComFormatacao() {
        assertThat(validator.isValid("529.982.247-25", null)).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar CNPJ valido sem formatacao")
    void deveAceitarCnpjValidoSemFormatacao() {
        assertThat(validator.isValid("11222333000181", null)).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar CNPJ valido com formatacao")
    void deveAceitarCnpjValidoComFormatacao() {
        assertThat(validator.isValid("11.222.333/0001-81", null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "12345678900"})
    @DisplayName("Deve rejeitar CPF invalido")
    void deveRejeitarCpfInvalido(String cpf) {
        assertThat(validator.isValid(cpf, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000000", "12345678000100"})
    @DisplayName("Deve rejeitar CNPJ invalido")
    void deveRejeitarCnpjInvalido(String cnpj) {
        assertThat(validator.isValid(cnpj, null)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar valor nulo")
    void deveRejeitarNulo() {
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar valor em branco")
    void deveRejeitarEmBranco() {
        assertThat(validator.isValid("   ", null)).isFalse();
    }
}
