package br.com.fiap.siase.domain.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlacaVeiculoValidator: validacao de placa Mercosul e antiga")
class PlacaVeiculoValidatorTest {

    private PlacaVeiculoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PlacaVeiculoValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC1D23", "XYZ9A00", "abc1d23"})
    @DisplayName("Deve aceitar placa Mercosul valida")
    void deveAceitarPlacaMercosulValida(String placa) {
        assertThat(validator.isValid(placa, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC1234", "XYZ9876", "abc-1234", "ABC-1234"})
    @DisplayName("Deve aceitar placa antiga valida com e sem hifen")
    void deveAceitarPlacaAntigaValida(String placa) {
        assertThat(validator.isValid(placa, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB12345", "ABCD123", "ABC123", "ABC12D3", "1234ABC"})
    @DisplayName("Deve rejeitar placa com formato invalido")
    void deveRejeitarPlacaInvalida(String placa) {
        assertThat(validator.isValid(placa, null)).isFalse();
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
