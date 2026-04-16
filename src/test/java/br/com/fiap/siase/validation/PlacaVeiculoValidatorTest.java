package br.com.fiap.siase.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlacaVeiculoValidator - Validação de Placas")
class PlacaVeiculoValidatorTest {

    private PlacaVeiculoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PlacaVeiculoValidator();
    }

    // -----------------------------------------------------------------------
    // FORMATO ANTIGO (ABC-1234 / ABC1234)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Formato antigo válido (ABC-1234)")
    class FormatoAntigoValido {

        @ParameterizedTest(name = "Placa \"{0}\" deve ser válida")
        @ValueSource(strings = {
                "ABC1234",
                "ABC-1234",
                "XYZ9999",
                "AAA0000",
        })
        void deveAceitarPlacaAntigaValida(String placa) {
            assertThat(validator.isValid(placa, null)).isTrue();
        }

        @Test
        @DisplayName("Deve aceitar placa antiga em minúsculas (normaliza para maiúsculas)")
        void deveAceitarMinusculas() {
            assertThat(validator.isValid("abc1234", null)).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // FORMATO MERCOSUL (ABC1D23)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Formato Mercosul válido (ABC1D23)")
    class FormatoMercosulValido {

        @ParameterizedTest(name = "Placa \"{0}\" deve ser válida")
        @ValueSource(strings = {
                "ABC1D23",
                "XYZ2A34",
                "CST9B99",
                "AAA0B00",
        })
        void deveAceitarPlacaMercosulValida(String placa) {
            assertThat(validator.isValid(placa, null)).isTrue();
        }

        @Test
        @DisplayName("Deve aceitar placa Mercosul em minúsculas")
        void deveAceitarMercosulMinusculas() {
            assertThat(validator.isValid("abc1d23", null)).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // PLACAS INVÁLIDAS
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Placas inválidas")
    class PlacasInvalidas {

        @ParameterizedTest(name = "Placa \"{0}\" deve ser rejeitada")
        @ValueSource(strings = {
                "AB1234",       // 2 letras no início (formato antigo exige 3)
                "ABCD1234",     // 4 letras no início
                "ABC123",       // 3 dígitos (formato antigo exige 4)
                "ABC12345",     // 5 dígitos
                "1BC1234",      // começa com número
                "ABC-123A",     // letra no fim do formato antigo
                "ABC1234D",     // 8 caracteres
                "",             // vazio
                "ABC 1234",     // espaço
        })
        void deveRejeitarPlacaInvalida(String placa) {
            assertThat(validator.isValid(placa, null)).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // CASOS NULOS E VAZIOS
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Valores nulos e vazios")
    class NulosEVazios {

        @Test
        @DisplayName("Deve rejeitar valor nulo")
        void deveRejeitarNulo() {
            assertThat(validator.isValid(null, null)).isFalse();
        }

        @Test
        @DisplayName("Deve rejeitar string vazia")
        void deveRejeitarVazio() {
            assertThat(validator.isValid("", null)).isFalse();
        }

        @Test
        @DisplayName("Deve rejeitar string com apenas espaços")
        void deveRejeitarApenasEspacos() {
            assertThat(validator.isValid("   ", null)).isFalse();
        }
    }
}
