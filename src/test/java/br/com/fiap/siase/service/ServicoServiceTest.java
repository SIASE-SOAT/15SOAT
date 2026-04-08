package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.ServicoInsumoRequest;
import br.com.fiap.siase.dto.request.ServicoRequest;
import br.com.fiap.siase.dto.response.ServicoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Peca;
import br.com.fiap.siase.model.Servico;
import br.com.fiap.siase.model.ServicoInsumo;
import br.com.fiap.siase.model.ServicoInsumoId;
import br.com.fiap.siase.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServicoService - Regras de Negócio")
class ServicoServiceTest {

    @Mock
    private ServicoRepository repository;

    @Mock
    private PecaService pecaService;

    @InjectMocks
    private ServicoService servicoService;

    private Servico servico;
    private UUID servicoId;

    @BeforeEach
    void setUp() {
        servicoId = UUID.randomUUID();
        servico = new Servico();
        servico.setNome("Troca de Óleo");
        servico.setDescricao("Substituição do óleo do motor");
        servico.setPreco(new BigDecimal("120.00"));
        servico.setAtivo(true);
        ReflectionTestUtils.setField(servico, "id", servicoId);
    }

    @Nested
    @DisplayName("Listagem de serviços")
    class Listagem {

        @Test
        @DisplayName("Deve listar apenas serviços ativos")
        void deveListarApenasAtivos() {
            when(repository.findByAtivoTrue()).thenReturn(List.of(servico));

            List<ServicoResponse> resultado = servicoService.listarAtivos();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).nome()).isEqualTo("Troca de Óleo");
            verify(repository).findByAtivoTrue();
            verify(repository, never()).findAll();
        }

        @Test
        @DisplayName("Deve listar todos os serviços incluindo inativos")
        void deveListarTodos() {
            Servico inativo = new Servico();
            inativo.setNome("Serviço Antigo");
            inativo.setPreco(BigDecimal.TEN);
            inativo.setAtivo(false);

            when(repository.findAll()).thenReturn(List.of(servico, inativo));

            List<ServicoResponse> resultado = servicoService.listarTodos();

            assertThat(resultado).hasSize(2);
            verify(repository).findAll();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há serviços ativos")
        void deveRetornarListaVaziaQuandoNaoHaAtivos() {
            when(repository.findByAtivoTrue()).thenReturn(List.of());

            List<ServicoResponse> resultado = servicoService.listarAtivos();

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("Busca por ID")
    class BuscaPorId {

        @Test
        @DisplayName("Deve retornar serviço quando encontrado")
        void deveRetornarQuandoEncontrado() {
            when(repository.findById(servicoId)).thenReturn(Optional.of(servico));

            ServicoResponse resultado = servicoService.buscarPorId(servicoId);

            assertThat(resultado.nome()).isEqualTo("Troca de Óleo");
            assertThat(resultado.preco()).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicoService.buscarPorId(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Serviço não encontrado");
        }
    }

    @Nested
    @DisplayName("Criação de serviço")
    class Criacao {

        @Test
        @DisplayName("Deve criar serviço com todos os campos preenchidos")
        void deveCriarComTodosCampos() {
            ServicoRequest request = new ServicoRequest("Alinhamento", "Alinhamento e balanceamento", new BigDecimal("80.00"));
            when(repository.save(any())).thenReturn(servico);

            ServicoResponse resultado = servicoService.criar(request);

            assertThat(resultado).isNotNull();
            verify(repository).save(any(Servico.class));
        }

        @Test
        @DisplayName("Deve criar serviço sem descrição")
        void deveCriarSemDescricao() {
            ServicoRequest request = new ServicoRequest("Balanceamento", null, new BigDecimal("60.00"));
            when(repository.save(any())).thenReturn(servico);

            servicoService.criar(request);

            verify(repository).save(argThat(s -> s.getDescricao() == null));
        }
    }

    @Nested
    @DisplayName("Atualização de serviço")
    class Atualizacao {

        @Test
        @DisplayName("Deve atualizar nome, descrição e preço")
        void deveAtualizarCampos() {
            ServicoRequest request = new ServicoRequest("Troca de Óleo Sintético", "Nova descrição", new BigDecimal("150.00"));
            when(repository.findById(servicoId)).thenReturn(Optional.of(servico));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServicoResponse resultado = servicoService.atualizar(servicoId, request);

            assertThat(resultado.nome()).isEqualTo("Troca de Óleo Sintético");
            assertThat(resultado.preco()).isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar serviço inexistente")
        void deveLancarExcecaoAoAtualizarInexistente() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicoService.atualizar(UUID.randomUUID(),
                    new ServicoRequest("X", null, BigDecimal.TEN)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Desativação de serviço")
    class Desativacao {

        @Test
        @DisplayName("Deve marcar serviço como inativo")
        void deveDesativarServico() {
            when(repository.findById(servicoId)).thenReturn(Optional.of(servico));
            when(repository.save(any())).thenReturn(servico);

            servicoService.desativar(servicoId);

            assertThat(servico.getAtivo()).isFalse();
            verify(repository).save(servico);
        }

        @Test
        @DisplayName("Deve lançar exceção ao desativar serviço inexistente")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicoService.desativar(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Gestão de insumos do serviço")
    class GestaoDeInsumos {

        private Peca peca;
        private UUID pecaId;

        @BeforeEach
        void setUpInsumo() {
            pecaId = UUID.randomUUID();
            peca = new Peca();
            peca.setCodigo("OL-5W30");
            peca.setNome("Óleo 5W30");
            peca.setPreco(new BigDecimal("45.00"));
            peca.setUnidadeMedida("L");
            peca.setQuantidadeEstoque(100);
            ReflectionTestUtils.setField(peca, "id", pecaId);
            servico.setInsumos(new ArrayList<>());
        }

        @Test
        @DisplayName("Deve adicionar insumo ao serviço")
        void deveAdicionarInsumo() {
            ServicoInsumoRequest request = new ServicoInsumoRequest(pecaId, 4);
            when(repository.findById(servicoId)).thenReturn(Optional.of(servico));
            when(pecaService.findOrThrow(pecaId)).thenReturn(peca);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServicoResponse resultado = servicoService.adicionarInsumo(servicoId, request);

            assertThat(resultado.insumos()).hasSize(1);
            assertThat(resultado.insumos().get(0).quantidade()).isEqualTo(4);
        }

        @Test
        @DisplayName("Deve lançar BusinessException ao adicionar insumo duplicado")
        void deveLancarExcecaoInsumoJaExiste() {
            ServicoInsumo insumoExistente = new ServicoInsumo();
            insumoExistente.setId(new ServicoInsumoId(servicoId, pecaId));
            insumoExistente.setPeca(peca);
            insumoExistente.setQuantidade(2);
            servico.getInsumos().add(insumoExistente);

            ServicoInsumoRequest request = new ServicoInsumoRequest(pecaId, 3);
            when(repository.findById(servicoId)).thenReturn(Optional.of(servico));
            when(pecaService.findOrThrow(pecaId)).thenReturn(peca);

            assertThatThrownBy(() -> servicoService.adicionarInsumo(servicoId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já está vinculada");
        }

        @Test
        @DisplayName("Deve atualizar quantidade de insumo existente")
        void deveAtualizarQuantidadeDeInsumo() {
            ServicoInsumo insumo = new ServicoInsumo();
            insumo.setId(new ServicoInsumoId(servicoId, pecaId));
            insumo.setPeca(peca);
            insumo.setQuantidade(2);
            servico.getInsumos().add(insumo);

            ServicoInsumoRequest request = new ServicoInsumoRequest(pecaId, 5);
            when(repository.findById(servicoId)).thenReturn(Optional.of(servico));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServicoResponse resultado = servicoService.atualizarInsumo(servicoId, pecaId, request);

            assertThat(resultado.insumos().get(0).quantidade()).isEqualTo(5);
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar insumo não vinculado")
        void deveLancarExcecaoAoAtualizarInsumoInexistente() {
            servico.setInsumos(new ArrayList<>());
            when(repository.findById(servicoId)).thenReturn(Optional.of(servico));

            assertThatThrownBy(() -> servicoService.atualizarInsumo(servicoId, pecaId,
                    new ServicoInsumoRequest(pecaId, 1)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve remover insumo vinculado ao serviço")
        void deveRemoverInsumo() {
            ServicoInsumo insumo = new ServicoInsumo();
            insumo.setId(new ServicoInsumoId(servicoId, pecaId));
            insumo.setPeca(peca);
            insumo.setQuantidade(2);
            servico.getInsumos().add(insumo);

            when(repository.findById(servicoId)).thenReturn(Optional.of(servico));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServicoResponse resultado = servicoService.removerInsumo(servicoId, pecaId);

            assertThat(resultado.insumos()).isEmpty();
        }

        @Test
        @DisplayName("Deve lançar exceção ao remover insumo não vinculado")
        void deveLancarExcecaoAoRemoverInsumoInexistente() {
            servico.setInsumos(new ArrayList<>());
            when(repository.findById(servicoId)).thenReturn(Optional.of(servico));

            assertThatThrownBy(() -> servicoService.removerInsumo(servicoId, UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
