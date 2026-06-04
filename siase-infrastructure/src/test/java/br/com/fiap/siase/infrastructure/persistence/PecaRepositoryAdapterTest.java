package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Peca;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("PecaRepositoryAdapter - Testes de integração com persistência")
class PecaRepositoryAdapterTest {

    @Autowired
    PecaRepositoryPort adapter;

    private Peca criarPecaAtiva(String codigo, int estoque) {
        return Peca.builder()
                .codigo(codigo)
                .nome("Filtro de Óleo")
                .descricao("Filtro de óleo para motor 1.6")
                .preco(new BigDecimal("45.90"))
                .quantidadeEstoque(estoque)
                .estoqueMinimo(2)
                .unidadeMedida("UN")
                .ativo(true)
                .build();
    }

    private Peca criarPecaInativa(String codigo) {
        return Peca.builder()
                .codigo(codigo)
                .nome("Vela de Ignição")
                .descricao("Vela de ignição iridium")
                .preco(new BigDecimal("29.90"))
                .quantidadeEstoque(0)
                .estoqueMinimo(1)
                .unidadeMedida("UN")
                .ativo(false)
                .build();
    }

    @Test
    @DisplayName("deve salvar e recuperar peça por ID com codigo, nome e preco corretos")
    void deveSalvarERecuperarPecaPorId() {
        Peca peca = criarPecaAtiva("FILTRO-001", 10);

        Peca salva = adapter.save(peca);

        assertThat(salva.getId()).isNotNull();

        Optional<Peca> encontrada = adapter.findById(salva.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getCodigo()).isEqualTo("FILTRO-001");
        assertThat(encontrada.get().getNome()).isEqualTo("Filtro de Óleo");
        assertThat(encontrada.get().getPreco()).isEqualByComparingTo(new BigDecimal("45.90"));
        assertThat(encontrada.get().getQuantidadeEstoque()).isEqualTo(10);
        assertThat(encontrada.get().getAtivo()).isTrue();
    }

    @Test
    @DisplayName("deve buscar apenas peças ativas ao chamar findByAtivoTrue")
    void deveBuscarApenasAtivas() {
        adapter.save(criarPecaAtiva("ATIVA-001", 5));
        adapter.save(criarPecaInativa("INATIVA-001"));

        List<Peca> ativas = adapter.findByAtivoTrue();

        assertThat(ativas).isNotEmpty();
        assertThat(ativas).allMatch(Peca::getAtivo);
        assertThat(ativas).noneMatch(p -> "INATIVA-001".equals(p.getCodigo()));
    }

    @Test
    @DisplayName("deve reservar estoque corretamente diminuindo a quantidade disponível")
    void deveReservarEstoqueCorretamente() {
        Peca peca = criarPecaAtiva("ESTOQUE-001", 10);
        Peca salva = adapter.save(peca);

        Peca paraAtualizar = adapter.findById(salva.getId()).orElseThrow();
        paraAtualizar.reservarEstoque(3);
        adapter.save(paraAtualizar);

        Optional<Peca> aposReserva = adapter.findById(salva.getId());

        assertThat(aposReserva).isPresent();
        assertThat(aposReserva.get().getQuantidadeEstoque()).isEqualTo(7);
    }

    @Test
    @DisplayName("deve lançar IllegalStateException quando estoque insuficiente para reserva")
    void deveLancarErroQuandoEstoqueInsuficiente() {
        Peca peca = criarPecaAtiva("BAIXO-001", 2);
        Peca salva = adapter.save(peca);

        Peca paraAtualizar = adapter.findById(salva.getId()).orElseThrow();

        assertThatThrownBy(() -> paraAtualizar.reservarEstoque(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Estoque insuficiente");
    }

    @Test
    @DisplayName("deve retornar vazio quando peça não encontrada por ID inexistente")
    void deveRetornarVazioQuandoPecaNaoEncontrada() {
        UUID idAleatorio = UUID.randomUUID();

        Optional<Peca> resultado = adapter.findById(idAleatorio);

        assertThat(resultado).isEmpty();
    }
}
