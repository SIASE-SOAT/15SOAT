package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.ClienteRequest;
import br.com.fiap.siase.dto.response.ClienteResponse;
import br.com.fiap.siase.exception.DuplicateResourceException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.enums.TipoPessoa;
import br.com.fiap.siase.repository.ClienteRepository;
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
@DisplayName("ClienteService - Regras de Negócio")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;
    private UUID clienteId;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
        cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", clienteId);
        ReflectionTestUtils.setField(cliente, "criadoEm", LocalDateTime.now());
        ReflectionTestUtils.setField(cliente, "atualizadoEm", LocalDateTime.now());
        cliente.setNome("João Silva");
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setDocumento("52998224725");
        cliente.setEmail("joao@email.com");
        cliente.setTelefone("11999999999");
        cliente.setEndereco("Rua A, 100");
        cliente.setAtivo(true);
    }

    private ClienteRequest buildRequest() {
        return new ClienteRequest("João Silva", TipoPessoa.PF, "52998224725",
                "joao@email.com", "11999999999", "Rua A, 100");
    }

    // -----------------------------------------------------------------------
    // CRIAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Criar cliente")
    class Criar {

        @Test
        @DisplayName("Deve criar cliente com sucesso")
        void deveCriarClienteComSucesso() {
            when(clienteRepository.existsByDocumento("52998224725")).thenReturn(false);
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

            ClienteResponse response = clienteService.criar(buildRequest());

            assertThat(response).isNotNull();
            assertThat(response.nome()).isEqualTo("João Silva");
            assertThat(response.tipoPessoa()).isEqualTo("PF");
            assertThat(response.documento()).isEqualTo("52998224725");
            verify(clienteRepository).save(any(Cliente.class));
        }

        @Test
        @DisplayName("Deve limpar máscara do documento antes de verificar duplicidade")
        void deveLimparDocumentoAntesDeVerificar() {
            ClienteRequest req = new ClienteRequest("Maria", TipoPessoa.PF, "529.982.247-25",
                    null, null, null);

            when(clienteRepository.existsByDocumento("52998224725")).thenReturn(false);
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

            clienteService.criar(req);

            verify(clienteRepository).existsByDocumento("52998224725");
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando documento já cadastrado")
        void deveLancarExcecaoQuandoDocumentoDuplicado() {
            when(clienteRepository.existsByDocumento("52998224725")).thenReturn(true);

            var request = buildRequest();
            assertThatThrownBy(() -> clienteService.criar(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("52998224725");

            verify(clienteRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // BUSCAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Buscar cliente")
    class Buscar {

        @Test
        @DisplayName("Deve retornar cliente por ID")
        void deveRetornarClientePorId() {
            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));

            ClienteResponse response = clienteService.buscarPorId(clienteId);

            assertThat(response.id()).isEqualTo(clienteId);
            assertThat(response.nome()).isEqualTo("João Silva");
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException para ID inexistente")
        void deveLancarExcecaoParaIdInexistente() {
            UUID idDesconhecido = UUID.randomUUID();
            when(clienteRepository.findById(idDesconhecido)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.buscarPorId(idDesconhecido))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve retornar cliente por documento")
        void deveRetornarClientePorDocumento() {
            when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.of(cliente));

            ClienteResponse response = clienteService.buscarPorDocumento("529.982.247-25");

            assertThat(response.documento()).isEqualTo("52998224725");
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException para documento inexistente")
        void deveLancarExcecaoParaDocumentoInexistente() {
            when(clienteRepository.findByDocumento(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.buscarPorDocumento("00000000000"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // LISTAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Listar clientes")
    class Listar {

        @Test
        @DisplayName("Deve retornar lista de todos os clientes")
        void deveRetornarListaTodos() {
            when(clienteRepository.findAll()).thenReturn(List.of(cliente));

            List<ClienteResponse> lista = clienteService.listarTodos();

            assertThat(lista).hasSize(1);
            assertThat(lista.get(0).nome()).isEqualTo("João Silva");
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há clientes")
        void deveRetornarListaVazia() {
            when(clienteRepository.findAll()).thenReturn(List.of());

            assertThat(clienteService.listarTodos()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // ATUALIZAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Atualizar cliente")
    class Atualizar {

        @Test
        @DisplayName("Deve atualizar dados do cliente")
        void deveAtualizarCliente() {
            ClienteRequest req = new ClienteRequest("João Atualizado", TipoPessoa.PF, "52998224725",
                    "novo@email.com", "11988888888", "Rua B, 200");

            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
                Cliente c = inv.getArgument(0);
                c.setNome("João Atualizado");
                return c;
            });

            ClienteResponse response = clienteService.atualizar(clienteId, req);

            assertThat(response.nome()).isEqualTo("João Atualizado");
            verify(clienteRepository).save(any(Cliente.class));
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar cliente inexistente")
        void deveLancarExcecaoAoAtualizarInexistente() {
            UUID idDesconhecido = UUID.randomUUID();
            when(clienteRepository.findById(idDesconhecido)).thenReturn(Optional.empty());

            var request = buildRequest();
            assertThatThrownBy(() -> clienteService.atualizar(idDesconhecido, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // DESATIVAR
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Desativar cliente")
    class Desativar {

        @Test
        @DisplayName("Deve desativar cliente (soft delete)")
        void deveDesativarCliente() {
            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

            clienteService.desativar(clienteId);

            assertThat(cliente.getAtivo()).isFalse();
            verify(clienteRepository).save(cliente);
        }

        @Test
        @DisplayName("Deve lançar exceção ao desativar cliente inexistente")
        void deveLancarExcecaoAoDesativarInexistente() {
            UUID idDesconhecido = UUID.randomUUID();
            when(clienteRepository.findById(idDesconhecido)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.desativar(idDesconhecido))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
