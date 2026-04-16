package br.com.fiap.siase.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CpfCnpjValidator - Validação de CPF e CNPJ")
class CpfCnpjValidatorTest {

    private CpfCnpjValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CpfCnpjValidator();
    }

    // -----------------------------------------------------------------------
    // CPF VÁLIDOS
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("CPF válidos")
    class CpfValidos {

        @ParameterizedTest(name = "CPF \"{0}\" deve ser válido")
        @ValueSource(strings = {
                "52998224725",      // sem máscara
                "529.982.247-25",   // com máscara
                "11144477735",      // outro CPF válido
                "111.444.777-35",   // com máscara
        })
        void deveAceitarCpfValido(String cpf) {
            assertThat(validator.isValid(cpf, null)).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // CPF INVÁLIDOS
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("CPF inválidos")
    class CpfInvalidos {

        @ParameterizedTest(name = "CPF \"{0}\" deve ser rejeitado")
        @ValueSource(strings = {
                "00000000000",      // todos iguais
                "11111111111",      // todos iguais
                "12345678901",      // dígitos inválidos
                "529.982.247-00",   // dígito verificador errado
        })
        void deveRejeitarCpfInvalido(String cpf) {
            assertThat(validator.isValid(cpf, null)).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // CNPJ VÁLIDOS
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("CNPJ válidos")
    class CnpjValidos {

        @ParameterizedTest(name = "CNPJ \"{0}\" deve ser válido")
        @ValueSource(strings = {
                "11222333000181",       // sem máscara
                "11.222.333/0001-81",   // com máscara
                "45997418000153",       // outro CNPJ válido
        })
        void deveAceitarCnpjValido(String cnpj) {
            assertThat(validator.isValid(cnpj, null)).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // CNPJ INVÁLIDOS
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("CNPJ inválidos")
    class CnpjInvalidos {

        @ParameterizedTest(name = "CNPJ \"{0}\" deve ser rejeitado")
        @ValueSource(strings = {
                "00000000000000",       // todos zeros
                "11111111111111",       // todos iguais
                "12345678901234",       // dígitos inválidos
                "11.222.333/0001-00",   // dígito verificador errado
        })
        void deveRejeitarCnpjInvalido(String cnpj) {
            assertThat(validator.isValid(cnpj, null)).isFalse();
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
