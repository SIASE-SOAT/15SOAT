package br.com.fiap.siase.application.usecase;

import br.com.fiap.siase.domain.exception.ResourceNotFoundException;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListarOrdensDoClienteUC: listagem por documento autenticado")
class ListarOrdensDoClienteUCTest {

  @Mock
  private OrdemServicoRepositoryPort ordemServicoRepository;

  @Mock
  private ClienteRepositoryPort clienteRepository;

  private ListarOrdensDoClienteUC useCase;
  private Cliente cliente;

  @BeforeEach
  void setUp() {
    useCase = new ListarOrdensDoClienteUC(ordemServicoRepository, clienteRepository);
    cliente = new Cliente();
    cliente.setId(UUID.randomUUID());
    cliente.setNome("Maria Silva");
    cliente.setDocumento("52998224725");
  }

  @Test
  @DisplayName("Deve normalizar CPF e retornar ordens da mais recente para a mais antiga")
  void deveListarOrdensOrdenadasPorDataDeAbertura() {
    var veiculo = new Veiculo();
    veiculo.setId(UUID.randomUUID());
    veiculo.setPlaca("ABC1234");
    veiculo.setModelo("Civic");

    var antiga = OrdemDeServico.builder()
            .id(UUID.randomUUID())
            .numero("OS-ANTIGA")
            .cliente(cliente)
            .veiculo(veiculo)
            .dataAbertura(LocalDateTime.of(2026, 1, 10, 10, 0))
            .build();
    var recente = OrdemDeServico.builder()
            .id(UUID.randomUUID())
            .numero("OS-RECENTE")
            .cliente(cliente)
            .veiculo(veiculo)
            .dataAbertura(LocalDateTime.of(2026, 2, 10, 10, 0))
            .build();

    when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.of(cliente));
    when(ordemServicoRepository.findByClienteId(cliente.getId())).thenReturn(List.of(antiga, recente));

    var resultado = useCase.executarPorDocumento("529.982.247-25");

    assertThat(resultado).extracting("numero").containsExactly("OS-RECENTE", "OS-ANTIGA");
    verify(clienteRepository).findByDocumento("52998224725");
    verify(ordemServicoRepository).findByClienteId(cliente.getId());
    verifyNoMoreInteractions(clienteRepository, ordemServicoRepository);
  }

  @Test
  @DisplayName("Deve lançar exceção quando o cliente não existe")
  void deveLancarExcecaoQuandoClienteNaoEncontrado() {
    when(clienteRepository.findByDocumento("00000000000")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executarPorDocumento("000.000.000-00"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Cliente não encontrado para o documento autenticado");

    verify(clienteRepository).findByDocumento("00000000000");
    verifyNoMoreInteractions(clienteRepository, ordemServicoRepository);
  }
}
