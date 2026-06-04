package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.enums.TipoPessoa;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("ClienteRepositoryAdapter - Testes de integração com persistência")
class ClienteRepositoryAdapterTest {

    @Autowired
    ClienteRepositoryPort adapter;

    private Cliente criarClienteValido(String documento) {
        return Cliente.builder()
                .nome("João da Silva")
                .tipoPessoa(TipoPessoa.PF)
                .documento(documento)
                .email("joao@example.com")
                .telefone("11999990000")
                .endereco("Rua Teste, 100")
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("deve salvar e recuperar cliente por ID com todos os campos")
    void deveSalvarERecuperarClientePorId() {
        Cliente cliente = criarClienteValido("52998224725");

        Cliente salvo = adapter.save(cliente);

        assertThat(salvo.getId()).isNotNull();

        Optional<Cliente> encontrado = adapter.findById(salvo.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(salvo.getId());
        assertThat(encontrado.get().getNome()).isEqualTo("João da Silva");
        assertThat(encontrado.get().getTipoPessoa()).isEqualTo(TipoPessoa.PF);
        assertThat(encontrado.get().getDocumento()).isEqualTo("52998224725");
        assertThat(encontrado.get().getEmail()).isEqualTo("joao@example.com");
        assertThat(encontrado.get().getTelefone()).isEqualTo("11999990000");
        assertThat(encontrado.get().getEndereco()).isEqualTo("Rua Teste, 100");
        assertThat(encontrado.get().getAtivo()).isTrue();
        assertThat(encontrado.get().getCriadoEm()).isNotNull();
    }

    @Test
    @DisplayName("deve buscar cliente por documento")
    void deveBuscarClientePorDocumento() {
        Cliente cliente = criarClienteValido("11122233344");
        adapter.save(cliente);

        Optional<Cliente> encontrado = adapter.findByDocumento("11122233344");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getDocumento()).isEqualTo("11122233344");
        assertThat(encontrado.get().getNome()).isEqualTo("João da Silva");
    }

    @Test
    @DisplayName("deve listar todos os clientes com pelo menos dois cadastrados")
    void deveListarTodosClientes() {
        adapter.save(criarClienteValido("11111111111"));
        adapter.save(criarClienteValido("22222222222"));

        List<Cliente> clientes = adapter.findAll();

        assertThat(clientes).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("deve remover cliente por ID via deleteById")
    void deveDesativarClienteViaSoftDelete() {
        Cliente salvo = adapter.save(criarClienteValido("33333333333"));
        UUID id = salvo.getId();

        adapter.deleteById(id);

        Optional<Cliente> aposDelete = adapter.findById(id);
        assertThat(aposDelete).isEmpty();
    }

    @Test
    @DisplayName("deve retornar vazio quando cliente não encontrado por ID inexistente")
    void deveRetornarVazioQuandoClienteNaoEncontrado() {
        UUID idAleatorio = UUID.randomUUID();

        Optional<Cliente> resultado = adapter.findById(idAleatorio);

        assertThat(resultado).isEmpty();
    }
}
