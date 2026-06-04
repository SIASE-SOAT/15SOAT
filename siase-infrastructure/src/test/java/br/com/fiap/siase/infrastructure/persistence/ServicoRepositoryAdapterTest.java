package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Servico;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("ServicoRepositoryAdapter - Testes de integração com persistência")
class ServicoRepositoryAdapterTest {

    @Autowired
    ServicoRepositoryPort adapter;

    private Servico criarServicoAtivo(String nome) {
        return Servico.builder()
                .nome(nome)
                .descricao("Troca completa do óleo do motor")
                .preco(new BigDecimal("120.00"))
                .tempoEstimadoMinutos(30)
                .ativo(true)
                .build();
    }

    private Servico criarServicoInativo(String nome) {
        return Servico.builder()
                .nome(nome)
                .descricao("Serviço descontinuado")
                .preco(new BigDecimal("80.00"))
                .tempoEstimadoMinutos(60)
                .ativo(false)
                .build();
    }

    @Test
    @DisplayName("deve salvar e recuperar serviço por ID com nome e preco corretos")
    void deveSalvarERecuperarServicoPorId() {
        Servico servico = criarServicoAtivo("Troca de Óleo");

        Servico salvo = adapter.save(servico);

        assertThat(salvo.getId()).isNotNull();

        Optional<Servico> encontrado = adapter.findById(salvo.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).isEqualTo("Troca de Óleo");
        assertThat(encontrado.get().getPreco()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(encontrado.get().getTempoEstimadoMinutos()).isEqualTo(30);
        assertThat(encontrado.get().getAtivo()).isTrue();
    }

    @Test
    @DisplayName("deve listar apenas serviços ativos ao chamar findByAtivoTrue")
    void deveListarApenasServicosAtivos() {
        adapter.save(criarServicoAtivo("Alinhamento"));
        adapter.save(criarServicoInativo("Serviço Antigo"));

        List<Servico> ativos = adapter.findByAtivoTrue();

        assertThat(ativos).isNotEmpty();
        assertThat(ativos).allMatch(Servico::getAtivo);
        assertThat(ativos).noneMatch(s -> "Serviço Antigo".equals(s.getNome()));
    }

    @Test
    @DisplayName("deve retornar todos os serviços incluindo inativos ao chamar findAll")
    void deveRetornarTodosServicos() {
        adapter.save(criarServicoAtivo("Balanceamento"));
        adapter.save(criarServicoInativo("Revisão Antiga"));

        List<Servico> todos = adapter.findAll();

        assertThat(todos).hasSizeGreaterThanOrEqualTo(2);

        boolean temAtivo = todos.stream().anyMatch(Servico::getAtivo);
        boolean temInativo = todos.stream().anyMatch(s -> !s.getAtivo());
        assertThat(temAtivo).isTrue();
        assertThat(temInativo).isTrue();
    }
}
