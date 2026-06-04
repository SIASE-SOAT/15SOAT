package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.enums.TipoPessoa;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("VeiculoRepositoryAdapter - Integração com banco de dados")
class VeiculoRepositoryAdapterTest {

    @Autowired
    private ClienteRepositoryPort clienteRepository;

    @Autowired
    private VeiculoRepositoryPort veiculoRepository;

    private Cliente salvarCliente(String nome, String documento) {
        Cliente cliente = Cliente.builder()
                .nome(nome)
                .tipoPessoa(TipoPessoa.PF)
                .documento(documento)
                .email(nome.toLowerCase().replace(" ", ".") + "@example.com")
                .ativo(true)
                .build();
        return clienteRepository.save(cliente);
    }

    @Test
    @DisplayName("deve salvar e recuperar veículo por ID")
    void deveSalvarERecuperarVeiculoPorId() {
        Cliente cliente = salvarCliente("Maria Souza", "12345678901");

        Veiculo veiculo = Veiculo.builder()
                .placa("ABC1D23")
                .marca("Toyota")
                .modelo("Corolla")
                .ano(2022)
                .cor("Prata")
                .ativo(true)
                .cliente(cliente)
                .build();

        Veiculo salvo = veiculoRepository.save(veiculo);

        assertThat(salvo.getId()).isNotNull();

        Optional<Veiculo> encontrado = veiculoRepository.findById(salvo.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getPlaca()).isEqualTo("ABC1D23");
        assertThat(encontrado.get().getMarca()).isEqualTo("Toyota");
        assertThat(encontrado.get().getModelo()).isEqualTo("Corolla");
        assertThat(encontrado.get().getAno()).isEqualTo(2022);
        assertThat(encontrado.get().getCor()).isEqualTo("Prata");
        assertThat(encontrado.get().getAtivo()).isTrue();
        assertThat(encontrado.get().getCliente()).isNotNull();
        assertThat(encontrado.get().getCliente().getId()).isEqualTo(cliente.getId());
    }

    @Test
    @DisplayName("deve buscar veículo por placa")
    void deveBuscarVeiculoPorPlaca() {
        Cliente cliente = salvarCliente("Carlos Lima", "98765432100");

        Veiculo veiculo = Veiculo.builder()
                .placa("XYZ9A87")
                .marca("Honda")
                .modelo("Civic")
                .ano(2021)
                .cor("Preto")
                .ativo(true)
                .cliente(cliente)
                .build();

        veiculoRepository.save(veiculo);

        Optional<Veiculo> encontrado = veiculoRepository.findByPlaca("XYZ9A87");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getPlaca()).isEqualTo("XYZ9A87");
        assertThat(encontrado.get().getMarca()).isEqualTo("Honda");
        assertThat(encontrado.get().getModelo()).isEqualTo("Civic");
    }

    @Test
    @DisplayName("deve buscar veículos por ID do cliente")
    void deveBuscarVeiculosPorClienteId() {
        Cliente cliente = salvarCliente("Ana Paula", "11122233344");

        Veiculo veiculo1 = Veiculo.builder()
                .placa("DEF2E45")
                .marca("Chevrolet")
                .modelo("Onix")
                .ano(2020)
                .cor("Branco")
                .ativo(true)
                .cliente(cliente)
                .build();

        Veiculo veiculo2 = Veiculo.builder()
                .placa("GHI3F67")
                .marca("Ford")
                .modelo("Ka")
                .ano(2019)
                .cor("Azul")
                .ativo(true)
                .cliente(cliente)
                .build();

        veiculoRepository.save(veiculo1);
        veiculoRepository.save(veiculo2);

        List<Veiculo> veiculos = veiculoRepository.findByClienteId(cliente.getId());

        assertThat(veiculos).hasSize(2);
        assertThat(veiculos).extracting(Veiculo::getPlaca)
                .containsExactlyInAnyOrder("DEF2E45", "GHI3F67");
    }

    @Test
    @DisplayName("deve retornar vazio quando veículo não encontrado pela placa")
    void deveRetornarVazioQuandoVeiculoNaoEncontrado() {
        Optional<Veiculo> resultado = veiculoRepository.findByPlaca("PLACA-INEXISTENTE");

        assertThat(resultado).isEmpty();
    }
}
