package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.MovimentacaoEstoqueRequest;
import br.com.fiap.siase.dto.request.PecaRequest;
import br.com.fiap.siase.dto.response.PecaResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Peca;
import br.com.fiap.siase.repository.PecaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PecaService - Regras de Negócio")
class PecaServiceTest {

    @Mock
    private PecaRepository repository;

    @InjectMocks
    private PecaService pecaService;

    private Peca peca;
    private UUID pecaId;

    @BeforeEach
    void setUp() {
        pecaId = UUID.randomUUID();
        peca = new Peca();
        peca.setCodigo("FILTRO-001");
        peca.setNome("Filtro de Óleo");
        peca.setPreco(new BigDecimal("35.00"));
        peca.setQuantidadeEstoque(50);
        peca.setEstoqueMinimo(10);
        peca.setUnidadeMedida("UN");
        peca.setAtivo(true);
    }

    @Nested
    @DisplayName("Listagem de peças")
    class Listagem {

        @Test
        @DisplayName("Deve listar apenas peças ativas")
        void deveListarApenasAtivas() {
            when(repository.findByAtivoTrue()).thenReturn(List.of(peca));

            List<PecaResponse> resultado = pecaService.listarAtivas();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).codigo()).isEqualTo("FILTRO-001");
            verify(repository).findByAtivoTrue();
        }

        @Test
        @DisplayName("Deve sinalizar quando estoque está abaixo do mínimo")
        void deveSinalizarEstoqueBaixo() {
            peca.setQuantidadeEstoque(5);
            peca.setEstoqueMinimo(10);
            when(repository.findByAtivoTrue()).thenReturn(List.of(peca));

            List<PecaResponse> resultado = pecaService.listarAtivas();

            assertThat(resultado.get(0).estoqueAbaixoMinimo()).isTrue();
        }

        @Test
        @DisplayName("Não deve sinalizar estoque baixo quando acima do mínimo")
        void naoDeveSinalizarQuandoEstoqueAdequado() {
            peca.setQuantidadeEstoque(50);
            peca.setEstoqueMinimo(10);
            when(repository.findByAtivoTrue()).thenReturn(List.of(peca));

            List<PecaResponse> resultado = pecaService.listarAtivas();

            assertThat(resultado.get(0).estoqueAbaixoMinimo()).isFalse();
        }
    }

    @Nested
    @DisplayName("Criação de peça")
    class Criacao {

        @Test
        @DisplayName("Deve normalizar código para maiúsculo")
        void deveNormalizarCodigoParaMaiusculo() {
            PecaRequest request = new PecaRequest("filtro-001", "Filtro de Óleo", null,
                    new BigDecimal("35.00"), 50, 10, "un");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            pecaService.criar(request);

            verify(repository).save(argThat(p ->
                    "FILTRO-001".equals(p.getCodigo()) && "UN".equals(p.getUnidadeMedida())));
        }

        @Test
        @DisplayName("Deve usar 'UN' como unidade padrão quando não informada")
        void deveUsarUnidadePadraoUN() {
            PecaRequest request = new PecaRequest("OL-001", "Óleo", null,
                    new BigDecimal("50.00"), null, null, null);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            pecaService.criar(request);

            verify(repository).save(argThat(p ->
                    "UN".equals(p.getUnidadeMedida()) &&
                    p.getQuantidadeEstoque() == 0 &&
                    p.getEstoqueMinimo() == 0));
        }

        @Test
        @DisplayName("Deve salvar todos os campos corretamente")
        void deveSalvarTodosCampos() {
            PecaRequest request = new PecaRequest("VELA-001", "Vela de Ignição",
                    "Vela NGK", new BigDecimal("25.00"), 20, 5, "UN");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            pecaService.criar(request);

            verify(repository).save(argThat(p ->
                    p.getNome().equals("Vela de Ignição") &&
                    p.getDescricao().equals("Vela NGK") &&
                    p.getPreco().compareTo(new BigDecimal("25.00")) == 0 &&
                    p.getQuantidadeEstoque() == 20 &&
                    p.getEstoqueMinimo() == 5));
        }
    }

    @Nested
    @DisplayName("Movimentação de estoque")
    class MovimentacaoDeEstoque {

        @Test
        @DisplayName("ENTRADA deve incrementar estoque")
        void entradaDeveIncrementarEstoque() {
            when(repository.findById(pecaId)).thenReturn(Optional.of(peca));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MovimentacaoEstoqueRequest request = new MovimentacaoEstoqueRequest(
                    MovimentacaoEstoqueRequest.Operacao.ENTRADA, 20);

            PecaResponse resultado = pecaService.movimentarEstoque(pecaId, request);

            assertThat(resultado.quantidadeEstoque()).isEqualTo(70);
        }

        @Test
        @DisplayName("SAIDA deve decrementar estoque")
        void saidaDeveDecrementarEstoque() {
            when(repository.findById(pecaId)).thenReturn(Optional.of(peca));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MovimentacaoEstoqueRequest request = new MovimentacaoEstoqueRequest(
                    MovimentacaoEstoqueRequest.Operacao.SAIDA, 10);

            PecaResponse resultado = pecaService.movimentarEstoque(pecaId, request);

            assertThat(resultado.quantidadeEstoque()).isEqualTo(40);
        }

        @Test
        @DisplayName("SAIDA deve lançar BusinessException quando estoque insuficiente")
        void saidaDeveLancarExcecaoEstoqueInsuficiente() {
            peca.setQuantidadeEstoque(5);
            when(repository.findById(pecaId)).thenReturn(Optional.of(peca));

            MovimentacaoEstoqueRequest request = new MovimentacaoEstoqueRequest(
                    MovimentacaoEstoqueRequest.Operacao.SAIDA, 10);

            assertThatThrownBy(() -> pecaService.movimentarEstoque(pecaId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Estoque insuficiente")
                    .hasMessageContaining("5")
                    .hasMessageContaining("10");
        }

        @Test
        @DisplayName("Não deve alterar estoque quando SAIDA falha por insuficiência")
        void naoDeveAlterarEstoqueEmFalha() {
            peca.setQuantidadeEstoque(3);
            when(repository.findById(pecaId)).thenReturn(Optional.of(peca));

            MovimentacaoEstoqueRequest request = new MovimentacaoEstoqueRequest(
                    MovimentacaoEstoqueRequest.Operacao.SAIDA, 10);

            assertThatThrownBy(() -> pecaService.movimentarEstoque(pecaId, request))
                    .isInstanceOf(BusinessException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando peça não encontrada")
        void deveLancarExcecaoQuandoPecaNaoEncontrada() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            MovimentacaoEstoqueRequest request = new MovimentacaoEstoqueRequest(
                    MovimentacaoEstoqueRequest.Operacao.ENTRADA, 5);

            var id = UUID.randomUUID();
            assertThatThrownBy(() -> pecaService.movimentarEstoque(id, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Desativação de peça")
    class Desativacao {

        @Test
        @DisplayName("Deve marcar peça como inativa")
        void deveDesativarPeca() {
            when(repository.findById(pecaId)).thenReturn(Optional.of(peca));
            when(repository.save(any())).thenReturn(peca);

            pecaService.desativar(pecaId);

            assertThat(peca.getAtivo()).isFalse();
            verify(repository).save(peca);
        }

        @Test
        @DisplayName("Deve lançar exceção ao desativar peça inexistente")
        void deveLancarExcecaoQuandoNaoEncontrada() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            var id = UUID.randomUUID();
            assertThatThrownBy(() -> pecaService.desativar(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Peça não encontrada");
        }
    }
}
