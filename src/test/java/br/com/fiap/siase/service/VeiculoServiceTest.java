package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.VeiculoRequest;
import br.com.fiap.siase.dto.response.VeiculoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.DuplicateResourceException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.Veiculo;
import br.com.fiap.siase.model.enums.TipoPessoa;
import br.com.fiap.siase.repository.VeiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VeiculoService - Regras de Negócio")
class VeiculoServiceTest {

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private VeiculoService veiculoService;

    private Veiculo veiculo;
    private Cliente cliente;
    private UUID veiculoId;
    private UUID clienteId;

    @BeforeEach
    void setUp() {
        veiculoId = UUID.randomUUID();
        clienteId = UUID.randomUUID();

        cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", clienteId);
        cliente.setNome("João Silva");
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setDocumento("52998224725");

        veiculo = new Veiculo();
        ReflectionTestUtils.setField(veiculo, "id", veiculoId);
        ReflectionTestUtils.setField(veiculo, "criadoEm", LocalDateTime.now());
        ReflectionTestUtils.setField(veiculo, "atualizadoEm", LocalDateTime.now());
        veiculo.setPlaca("ABC1234");
        veiculo.setMarca("Toyota");
        veiculo.setModelo("Corolla");
        veiculo.setAno(2022);
        veiculo.setCor("Prata");
        veiculo.setAtivo(true);
        veiculo.setCliente(cliente);
    }

    private VeiculoRequest buildRequest() {
        return new VeiculoRequest("ABC1234", "Toyota", "Corolla", 2022, "Prata", clienteId);
    }

    // -----------------------------------------------------------------------
    // CRIAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Criar veículo")
    class Criar {

        @Test
        @DisplayName("Deve criar veículo com sucesso")
        void deveCriarComSucesso() {
            when(veiculoRepository.existsByPlaca("ABC1234")).thenReturn(false);
            when(clienteService.findById(clienteId)).thenReturn(cliente);
            when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);

            VeiculoResponse response = veiculoService.criar(buildRequest());

            assertThat(response).isNotNull();
            assertThat(response.placa()).isEqualTo("ABC1234");
            assertThat(response.marca()).isEqualTo("Toyota");
            assertThat(response.clienteId()).isEqualTo(clienteId);
            verify(veiculoRepository).save(any(Veiculo.class));
        }

        @Test
        @DisplayName("Deve normalizar placa para maiúsculas")
        void deveNormalizarPlaca() {
            VeiculoRequest req = new VeiculoRequest("abc1234", "Toyota", "Corolla", 2022, "Prata", clienteId);

            when(veiculoRepository.existsByPlaca("ABC1234")).thenReturn(false);
            when(clienteService.findById(clienteId)).thenReturn(cliente);
            when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);

            veiculoService.criar(req);

            verify(veiculoRepository).existsByPlaca("ABC1234");
        }

        @Test
        @DisplayName("Deve lançar BusinessException para placa duplicada")
        void deveLancarExcecaoParaPlacaDuplicada() {
            when(veiculoRepository.existsByPlaca("ABC1234")).thenReturn(true);

            assertThatThrownBy(() -> veiculoService.criar(buildRequest()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("ABC1234");

            verify(veiculoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando cliente não encontrado")
        void deveLancarExcecaoQuandoClienteNaoEncontrado() {
            when(veiculoRepository.existsByPlaca("ABC1234")).thenReturn(false);
            when(clienteService.findById(clienteId))
                    .thenThrow(new ResourceNotFoundException("Cliente", clienteId));

            assertThatThrownBy(() -> veiculoService.criar(buildRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // BUSCAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Buscar veículo")
    class Buscar {

        @Test
        @DisplayName("Deve retornar veículo por ID")
        void deveRetornarPorId() {
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));

            VeiculoResponse response = veiculoService.buscarPorId(veiculoId);

            assertThat(response.id()).isEqualTo(veiculoId);
            assertThat(response.placa()).isEqualTo("ABC1234");
        }

        @Test
        @DisplayName("Deve lançar exceção para ID inexistente")
        void deveLancarExcecaoPorIdInexistente() {
            when(veiculoRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> veiculoService.buscarPorId(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve retornar veículo por placa")
        void deveRetornarPorPlaca() {
            when(veiculoRepository.findByPlaca("ABC1234")).thenReturn(Optional.of(veiculo));

            VeiculoResponse response = veiculoService.buscarPorPlaca("abc1234");

            assertThat(response.placa()).isEqualTo("ABC1234");
        }

        @Test
        @DisplayName("Deve lançar exceção para placa inexistente")
        void deveLancarExcecaoPorPlacaInexistente() {
            when(veiculoRepository.findByPlaca(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> veiculoService.buscarPorPlaca("XYZ9999"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // LISTAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Listar veículos")
    class Listar {

        @Test
        @DisplayName("Deve listar todos os veículos")
        void deveListarTodos() {
            when(veiculoRepository.findAll()).thenReturn(List.of(veiculo));

            List<VeiculoResponse> lista = veiculoService.listarTodos();

            assertThat(lista).hasSize(1);
            assertThat(lista.get(0).placa()).isEqualTo("ABC1234");
        }

        @Test
        @DisplayName("Deve listar veículos por cliente")
        void deveListarPorCliente() {
            when(veiculoRepository.findByClienteId(clienteId)).thenReturn(List.of(veiculo));

            List<VeiculoResponse> lista = veiculoService.listarPorCliente(clienteId);

            assertThat(lista).hasSize(1);
            assertThat(lista.get(0).clienteId()).isEqualTo(clienteId);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando cliente não tem veículos")
        void deveRetornarListaVazia() {
            when(veiculoRepository.findByClienteId(clienteId)).thenReturn(List.of());

            assertThat(veiculoService.listarPorCliente(clienteId)).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // ATUALIZAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Atualizar veículo")
    class Atualizar {

        @Test
        @DisplayName("Deve atualizar dados do veículo")
        void deveAtualizarVeiculo() {
            VeiculoRequest req = new VeiculoRequest("ABC1234", "Honda", "Civic", 2023, "Branco", clienteId);

            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);

            VeiculoResponse response = veiculoService.atualizar(veiculoId, req);

            assertThat(response).isNotNull();
            verify(veiculoRepository).save(any(Veiculo.class));
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar veículo inexistente")
        void deveLancarExcecaoAoAtualizarInexistente() {
            when(veiculoRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> veiculoService.atualizar(UUID.randomUUID(), buildRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // DESATIVAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Desativar veículo")
    class Desativar {

        @Test
        @DisplayName("Deve desativar veículo (soft delete)")
        void deveDesativarVeiculo() {
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);

            veiculoService.desativar(veiculoId);

            assertThat(veiculo.getAtivo()).isFalse();
            verify(veiculoRepository).save(veiculo);
        }

        @Test
        @DisplayName("Deve lançar exceção ao desativar veículo inexistente")
        void deveLancarExcecaoAoDesativarInexistente() {
            when(veiculoRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> veiculoService.desativar(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
