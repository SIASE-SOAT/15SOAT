package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.Veiculo;
import br.com.fiap.siase.model.enums.TipoPessoa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("VeiculoRepository - Integração")
class VeiculoRepositoryIntegrationTest {

    @Autowired
    private VeiculoRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private Cliente cliente;
    private Veiculo corolla;
    private Veiculo civic;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setNome("João Silva");
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setDocumento("529.982.247-25");
        cliente.setAtivo(true);
        entityManager.persistAndFlush(cliente);

        corolla = veiculo("ABC1234", "Toyota",  "Corolla", 2022, cliente);
        civic   = veiculo("XYZ5678", "Honda",   "Civic",   2021, cliente);
        repository.saveAll(List.of(corolla, civic));
        entityManager.flush();
    }

    @Nested
    @DisplayName("findByPlaca")
    class FindByPlaca {

        @Test
        @DisplayName("Deve encontrar veículo pela placa")
        void deveEncontrarPorPlaca() {
            Optional<Veiculo> resultado = repository.findByPlaca("ABC1234");

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getModelo()).isEqualTo("Corolla");
        }

        @Test
        @DisplayName("Deve retornar vazio para placa inexistente")
        void deveRetornarVazioParaPlacaInexistente() {
            Optional<Veiculo> resultado = repository.findByPlaca("ZZZ9999");

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByPlaca")
    class ExistsByPlaca {

        @Test
        @DisplayName("Deve retornar true para placa existente")
        void deveRetornarTrueParaPlacaExistente() {
            assertThat(repository.existsByPlaca("ABC1234")).isTrue();
        }

        @Test
        @DisplayName("Deve retornar false para placa inexistente")
        void deveRetornarFalseParaPlacaInexistente() {
            assertThat(repository.existsByPlaca("ZZZ0000")).isFalse();
        }
    }

    @Nested
    @DisplayName("findByClienteId")
    class FindByClienteId {

        @Test
        @DisplayName("Deve listar todos os veículos do cliente")
        void deveListarVeiculosDoCliente() {
            List<Veiculo> resultado = repository.findByClienteId(cliente.getId());

            assertThat(resultado).hasSize(2)
                    .extracting(Veiculo::getPlaca)
                    .containsExactlyInAnyOrder("ABC1234", "XYZ5678");
        }

        @Test
        @DisplayName("Deve retornar lista vazia para cliente sem veículos")
        void deveRetornarVazioParaClienteSemVeiculos() {
            Cliente outro = new Cliente();
            outro.setNome("Maria");
            outro.setTipoPessoa(TipoPessoa.PF);
            outro.setDocumento("111.222.333-44");
            outro.setAtivo(true);
            entityManager.persistAndFlush(outro);

            List<Veiculo> resultado = repository.findByClienteId(outro.getId());

            assertThat(resultado).isEmpty();
        }
    }

    private Veiculo veiculo(String placa, String marca, String modelo, int ano, Cliente cliente) {
        Veiculo v = new Veiculo();
        v.setPlaca(placa);
        v.setMarca(marca);
        v.setModelo(modelo);
        v.setAno(ano);
        v.setAtivo(true);
        v.setCliente(cliente);
        return v;
    }
}
