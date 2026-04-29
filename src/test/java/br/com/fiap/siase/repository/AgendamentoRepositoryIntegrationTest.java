package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Agendamento;
import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.Veiculo;
import br.com.fiap.siase.model.enums.StatusAgendamento;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AgendamentoRepository - Integração")
class AgendamentoRepositoryIntegrationTest {

    @Autowired
    private AgendamentoRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private Cliente cliente;
    private Veiculo veiculo;
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 5, 1, 10, 0);

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setNome("João Silva");
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setDocumento("529.982.247-25");
        cliente.setAtivo(true);
        entityManager.persistAndFlush(cliente);

        veiculo = new Veiculo();
        veiculo.setPlaca("ABC1234");
        veiculo.setMarca("Toyota");
        veiculo.setModelo("Corolla");
        veiculo.setAno(2022);
        veiculo.setAtivo(true);
        veiculo.setCliente(cliente);
        entityManager.persistAndFlush(veiculo);

        Agendamento agendado    = agendamento(BASE,           StatusAgendamento.AGENDADO);
        Agendamento confirmado  = agendamento(BASE.plusHours(2), StatusAgendamento.CONFIRMADO);
        Agendamento cancelado   = agendamento(BASE.plusDays(1),  StatusAgendamento.CANCELADO);
        repository.saveAll(List.of(agendado, confirmado, cancelado));
        entityManager.flush();
    }

    @Nested
    @DisplayName("findByClienteId")
    class FindByClienteId {

        @Test
        @DisplayName("Deve listar todos os agendamentos do cliente")
        void deveListarAgendamentosDoCliente() {
            List<Agendamento> resultado = repository.findByClienteId(cliente.getId());

            assertThat(resultado).hasSize(3);
        }

        @Test
        @DisplayName("Deve retornar vazio para cliente sem agendamentos")
        void deveRetornarVazioParaClienteSemAgendamentos() {
            Cliente outro = new Cliente();
            outro.setNome("Maria");
            outro.setTipoPessoa(TipoPessoa.PF);
            outro.setDocumento("111.222.333-44");
            outro.setAtivo(true);
            entityManager.persistAndFlush(outro);

            assertThat(repository.findByClienteId(outro.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("Deve filtrar agendamentos por status AGENDADO")
        void deveFiltrarPorStatusAgendado() {
            List<Agendamento> resultado = repository.findByStatus(StatusAgendamento.AGENDADO);

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve filtrar agendamentos por status CANCELADO")
        void deveFiltrarPorStatusCancelado() {
            List<Agendamento> resultado = repository.findByStatus(StatusAgendamento.CANCELADO);

            assertThat(resultado).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByDataHoraBetween")
    class FindByDataHoraBetween {

        @Test
        @DisplayName("Deve retornar agendamentos dentro do intervalo")
        void deveRetornarAgendamentosDentroDoIntervalo() {
            List<Agendamento> resultado = repository.findByDataHoraBetween(
                    BASE.minusMinutes(1), BASE.plusHours(3));

            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Deve retornar vazio para intervalo sem agendamentos")
        void deveRetornarVazioParaIntervalVazio() {
            List<Agendamento> resultado = repository.findByDataHoraBetween(
                    BASE.plusDays(10), BASE.plusDays(20));

            assertThat(resultado).isEmpty();
        }
    }

    private Agendamento agendamento(LocalDateTime dataHora, StatusAgendamento status) {
        Agendamento a = new Agendamento();
        a.setCliente(cliente);
        a.setVeiculo(veiculo);
        a.setDataHora(dataHora);
        a.setStatus(status);
        return a;
    }
}
