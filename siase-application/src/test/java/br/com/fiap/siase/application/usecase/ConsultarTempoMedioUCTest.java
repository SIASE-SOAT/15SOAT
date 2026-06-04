package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarTempoMedioUC: consulta de tempo medio de execucao")
class ConsultarTempoMedioUCTest {

    @Mock private OrdemServicoRepositoryPort ordemServicoRepository;

    private ConsultarTempoMedioUC useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarTempoMedioUC(ordemServicoRepository);
    }

    @Test
    @DisplayName("Deve retornar tempo medio quando ha dados")
    void deveRetornarTempoMedioQuandoHaDados() {
        when(ordemServicoRepository.calcularTempoMedioExecucaoMinutos()).thenReturn(Optional.of(90.5));

        Double resultado = useCase.executar();

        assertThat(resultado).isEqualTo(90.5);
    }

    @Test
    @DisplayName("Deve retornar zero quando nao ha dados")
    void deveRetornarZeroQuandoNaoHaDados() {
        when(ordemServicoRepository.calcularTempoMedioExecucaoMinutos()).thenReturn(Optional.empty());

        Double resultado = useCase.executar();

        assertThat(resultado).isEqualTo(0.0);
    }
}
