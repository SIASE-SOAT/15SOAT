package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.ItemPecaRequest;
import br.com.fiap.siase.dto.request.ItemServicoRequest;
import br.com.fiap.siase.dto.request.OrdemDeServicoRequest;
import br.com.fiap.siase.dto.response.OrdemDeServicoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.*;
import br.com.fiap.siase.model.enums.StatusOS;
import br.com.fiap.siase.model.enums.TipoPessoa;
import br.com.fiap.siase.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrdemDeServicoService - Regras de Negócio")
class OrdemDeServicoServiceTest {

    @Mock private OrdemDeServicoRepository repository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private VeiculoRepository veiculoRepository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private PecaRepository pecaRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private OrdemDeServicoService service;

    private UUID clienteId;
    private UUID veiculoId;
    private UUID servicoId;
    private UUID pecaId;
    private UUID osId;

    private Cliente cliente;
    private Veiculo veiculo;
    private Servico servico;
    private Peca peca;
    private OrdemDeServico os;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
        servicoId = UUID.randomUUID();
        pecaId    = UUID.randomUUID();
        osId      = UUID.randomUUID();

        cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", clienteId);
        cliente.setNome("João da Silva");
        cliente.setDocumento("52998224725");
        cliente.setTipoPessoa(TipoPessoa.PF);

        veiculo = new Veiculo();
        ReflectionTestUtils.setField(veiculo, "id", veiculoId);
        veiculo.setPlaca("ABC1234");
        veiculo.setMarca("Toyota");
        veiculo.setModelo("Corolla");
        veiculo.setAno(2022);
        veiculo.setCliente(cliente);

        servico = new Servico();
        ReflectionTestUtils.setField(servico, "id", servicoId);
        servico.setNome("Troca de Óleo");
        servico.setPreco(new BigDecimal("120.00"));
        servico.setAtivo(true);

        peca = new Peca();
        ReflectionTestUtils.setField(peca, "id", pecaId);
        peca.setCodigo("FILTRO-001");
        peca.setNome("Filtro de Óleo");
        peca.setPreco(new BigDecimal("45.90"));
        peca.setQuantidadeEstoque(50);
        peca.setEstoqueMinimo(10);
        peca.setUnidadeMedida("UN");

        os = new OrdemDeServico();
        ReflectionTestUtils.setField(os, "id", osId);
        ReflectionTestUtils.setField(os, "criadoEm", LocalDateTime.now());
        ReflectionTestUtils.setField(os, "atualizadoEm", LocalDateTime.now());
        os.setNumero("OS-20260412-ABCDEF");
        os.setCliente(cliente);
        os.setVeiculo(veiculo);
        os.setStatus(StatusOS.RECEBIDA);
        os.setDataAbertura(LocalDateTime.now());
        os.setTotalServicos(BigDecimal.ZERO);
        os.setTotalPecas(BigDecimal.ZERO);
        os.setTotal(BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------ //
    //  Criação de OS                                                       //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Criar OS")
    class CriarOS {

        @Test
        @DisplayName("Deve criar OS com número gerado automaticamente no formato correto")
        void deveCriarOSComNumeroGerado() {
            stubCriarBasico();

            OrdemDeServicoRequest request = requestSomenteServico();
            service.criar(request);

            ArgumentCaptor<OrdemDeServico> captor = ArgumentCaptor.forClass(OrdemDeServico.class);
            verify(repository).save(captor.capture());

            assertThat(captor.getValue().getNumero())
                    .matches("OS-\\d{8}-[A-F0-9]{6}");
        }

        @Test
        @DisplayName("Deve fazer snapshot do preço do serviço no momento da criação")
        void deveFazerSnapshotDoPrecoDoServico() {
            stubCriarBasico();
            servico.setPreco(new BigDecimal("120.00"));

            service.criar(requestSomenteServico());

            ArgumentCaptor<OrdemDeServico> captor = ArgumentCaptor.forClass(OrdemDeServico.class);
            verify(repository).save(captor.capture());

            OrdemDeServico salvo = captor.getValue();
            assertThat(salvo.getItensServico()).hasSize(1);
            assertThat(salvo.getItensServico().get(0).getPrecoUnitario())
                    .isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("Deve fazer snapshot do preço da peça no momento da criação")
        void deveFazerSnapshotDoPrecoDaPeca() {
            stubCriarComPeca();
            peca.setPreco(new BigDecimal("45.90"));

            service.criar(requestComPeca(2));

            ArgumentCaptor<OrdemDeServico> captor = ArgumentCaptor.forClass(OrdemDeServico.class);
            verify(repository).save(captor.capture());

            OrdemDeServico salvo = captor.getValue();
            assertThat(salvo.getItensPeca()).hasSize(1);
            assertThat(salvo.getItensPeca().get(0).getPrecoUnitario())
                    .isEqualByComparingTo("45.90");
        }

        @Test
        @DisplayName("Deve calcular totais automaticamente na criação")
        void deveCalcularTotaisNaCriacao() {
            stubCriarComPeca();
            servico.setPreco(new BigDecimal("120.00"));
            peca.setPreco(new BigDecimal("45.90"));

            service.criar(requestComPeca(2));

            ArgumentCaptor<OrdemDeServico> captor = ArgumentCaptor.forClass(OrdemDeServico.class);
            verify(repository).save(captor.capture());

            OrdemDeServico salvo = captor.getValue();
            assertThat(salvo.getTotalServicos()).isEqualByComparingTo("120.00");
            assertThat(salvo.getTotalPecas()).isEqualByComparingTo("91.80");   // 45.90 * 2
            assertThat(salvo.getTotal()).isEqualByComparingTo("211.80");
        }

        @Test
        @DisplayName("Deve deduzir estoque da peça ao criar OS")
        void deveDeduzirEstoqueAoCriarOS() {
            stubCriarComPeca();
            peca.setQuantidadeEstoque(10);

            service.criar(requestComPeca(3));

            assertThat(peca.getQuantidadeEstoque()).isEqualTo(7);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Deve criar OS sem peças quando itensPeca é nulo")
        void deveCriarOSSemPecas() {
            stubCriarBasico();

            service.criar(requestSomenteServico());

            verify(pecaRepository, never()).save(any());
            ArgumentCaptor<OrdemDeServico> captor = ArgumentCaptor.forClass(OrdemDeServico.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getItensPeca()).isEmpty();
        }

        @Test
        @DisplayName("Deve vincular cliente e veículo corretos na OS")
        void deveVincularClienteEVeiculoCorretos() {
            stubCriarBasico();

            service.criar(requestSomenteServico());

            ArgumentCaptor<OrdemDeServico> captor = ArgumentCaptor.forClass(OrdemDeServico.class);
            verify(repository).save(captor.capture());

            assertThat(captor.getValue().getCliente().getId()).isEqualTo(clienteId);
            assertThat(captor.getValue().getVeiculo().getId()).isEqualTo(veiculoId);
        }

        @Test
        @DisplayName("Deve iniciar OS com status RECEBIDA")
        void deveIniciarComStatusRecebida() {
            stubCriarBasico();

            service.criar(requestSomenteServico());

            ArgumentCaptor<OrdemDeServico> captor = ArgumentCaptor.forClass(OrdemDeServico.class);
            verify(repository).save(captor.capture());

            assertThat(captor.getValue().getStatus()).isEqualTo(StatusOS.RECEBIDA);
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando cliente não existe")
        void deveLancarExcecaoQuandoClienteNaoExiste() {
            when(clienteRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.criar(requestSomenteServico()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cliente não encontrado");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando veículo não existe")
        void deveLancarExcecaoQuandoVeiculoNaoExiste() {
            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.criar(requestSomenteServico()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Veículo não encontrado");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando veículo pertence a outro cliente")
        void deveLancarExcecaoQuandoVeiculoDeOutroCliente() {
            Cliente outroCliente = new Cliente();
            ReflectionTestUtils.setField(outroCliente, "id", UUID.randomUUID());
            veiculo.setCliente(outroCliente);

            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));

            assertThatThrownBy(() -> service.criar(requestSomenteServico()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("veículo informado não pertence ao cliente");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando serviço não existe")
        void deveLancarExcecaoQuandoServicoNaoExiste() {
            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(servicoRepository.findById(servicoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.criar(requestSomenteServico()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Serviço não encontrado");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando peça não existe")
        void deveLancarExcecaoQuandoPecaNaoExiste() {
            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.criar(requestComPeca(1)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Peça não encontrada");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando estoque insuficiente")
        void deveLancarExcecaoQuandoEstoqueInsuficiente() {
            peca.setQuantidadeEstoque(2);

            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));

            assertThatThrownBy(() -> service.criar(requestComPeca(5)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Estoque insuficiente");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve salvar OS quando estoque insuficiente")
        void naoDeveSalvarOSQuandoEstoqueInsuficiente() {
            peca.setQuantidadeEstoque(1);

            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));

            assertThatThrownBy(() -> service.criar(requestComPeca(10)))
                    .isInstanceOf(BusinessException.class);

            verify(repository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ //
    //  Avancar status                                                      //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Avançar status")
    class AvancarStatus {

        @Test
        @DisplayName("Deve avançar do status RECEBIDA para EM_DIAGNOSTICO")
        void deveAvancarDeRecebidaParaEmDiagnostico() {
            os.setStatus(StatusOS.RECEBIDA);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(repository.save(any())).thenReturn(os);

            OrdemDeServicoResponse response = service.avancarStatus(osId);

            assertThat(response.status()).isEqualTo(StatusOS.EM_DIAGNOSTICO.name());
            verify(repository).save(os);
        }

        @Test
        @DisplayName("Deve percorrer todos os status até ENTREGUE em sequência")
        void devePercorrerTodosOsStatus() {
            os.setStatus(StatusOS.RECEBIDA);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.avancarStatus(osId);
            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);

            service.avancarStatus(osId);
            assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);

            service.avancarStatus(osId);
            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);

            service.avancarStatus(osId);
            assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);

            service.avancarStatus(osId);
            assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);
        }

        @Test
        @DisplayName("Deve definir dataFechamento ao avançar para ENTREGUE")
        void deveDefinirDataFechamentoAoEntregar() {
            os.setStatus(StatusOS.FINALIZADA);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(repository.save(any())).thenReturn(os);

            service.avancarStatus(osId);

            assertThat(os.getDataFechamento()).isNotNull();
        }

        @Test
        @DisplayName("Deve lançar BusinessException ao avançar OS já entregue")
        void deveLancarExcecaoAoAvancarOSEntregue() {
            os.setStatus(StatusOS.ENTREGUE);
            when(repository.findById(osId)).thenReturn(Optional.of(os));

            assertThatThrownBy(() -> service.avancarStatus(osId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já foi entregue");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando OS não existe")
        void deveLancarExcecaoQuandoOSNaoExiste() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.avancarStatus(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("OS não encontrada");
        }
    }

    // ------------------------------------------------------------------ //
    //  Consultas                                                           //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("Deve buscar OS por ID")
        void deveBuscarPorId() {
            when(repository.findById(osId)).thenReturn(Optional.of(os));

            OrdemDeServicoResponse response = service.buscarPorId(osId);

            assertThat(response.id()).isEqualTo(osId);
            assertThat(response.numero()).isEqualTo("OS-20260412-ABCDEF");
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException ao buscar por ID inexistente")
        void deveLancarExcecaoQuandoIdNaoExiste() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("OS não encontrada");
        }

        @Test
        @DisplayName("Deve buscar OS por número (endpoint público)")
        void deveBuscarPorNumero() {
            when(repository.findByNumero("OS-20260412-ABCDEF")).thenReturn(Optional.of(os));

            OrdemDeServicoResponse response = service.buscarPorNumero("OS-20260412-ABCDEF");

            assertThat(response.numero()).isEqualTo("OS-20260412-ABCDEF");
            assertThat(response.status()).isEqualTo(StatusOS.RECEBIDA.name());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException ao buscar por número inexistente")
        void deveLancarExcecaoQuandoNumeroNaoExiste() {
            when(repository.findByNumero(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorNumero("OS-INEXISTENTE"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("OS não encontrada");
        }

        @Test
        @DisplayName("Deve listar todas as OS")
        void deveListarTodasAsOS() {
            when(repository.findAll()).thenReturn(List.of(os));

            List<OrdemDeServicoResponse> resultado = service.listar();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).numero()).isEqualTo("OS-20260412-ABCDEF");
        }

        @Test
        @DisplayName("Deve listar OS filtradas por status")
        void deveListarPorStatus() {
            os.setStatus(StatusOS.EM_EXECUCAO);
            when(repository.findByStatus(StatusOS.EM_EXECUCAO)).thenReturn(List.of(os));

            List<OrdemDeServicoResponse> resultado = service.listarPorStatus(StatusOS.EM_EXECUCAO);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).status()).isEqualTo(StatusOS.EM_EXECUCAO.name());
            verify(repository).findByStatus(StatusOS.EM_EXECUCAO);
        }
    }

    // ------------------------------------------------------------------ //
    //  Adicionar Peça à Ordem                                             //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Adicionar peça à ordem")
    class AdicionarPecaAOrdem {

        @BeforeEach
        void setUpPeca() {
            peca.setAtivo(true);
            peca.setQuantidadeEstoque(100);
        }

        @Test
        @DisplayName("Deve adicionar peça com sucesso e recalcular totais")
        void deveAdicionarPecaComSucesso() {
            os.setStatus(StatusOS.RECEBIDA);
            os.setTotalPecas(BigDecimal.ZERO);
            os.setTotal(BigDecimal.ZERO);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));
            when(repository.save(any())).thenReturn(os);

            service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 2));

            assertThat(os.getItensPeca()).hasSize(1);
            assertThat(os.getItensPeca().get(0).getQuantidade()).isEqualTo(2);
            assertThat(os.getItensPeca().get(0).getPrecoUnitario()).isEqualByComparingTo("45.90");
            assertThat(os.getTotalPecas()).isEqualByComparingTo("91.80");
            verify(repository).save(os);
        }

        @Test
        @DisplayName("Deve fazer snapshot do preço da peça no momento da adição")
        void deveFazerSnapshotDoPrecoDaPeca() {
            os.setStatus(StatusOS.RECEBIDA);
            peca.setPreco(new BigDecimal("50.00"));
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));
            when(repository.save(any())).thenReturn(os);

            service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 1));

            ItemPeca itemAdicionado = os.getItensPeca().get(0);
            assertThat(itemAdicionado.getPrecoUnitario()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("Deve reservar estoque ao adicionar peça")
        void deveReservarEstoque() {
            os.setStatus(StatusOS.RECEBIDA);
            peca.setQuantidadeEstoque(100);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));
            when(repository.save(any())).thenReturn(os);

            service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 10));

            assertThat(peca.getQuantidadeEstoque()).isEqualTo(90);
        }

        @Test
        @DisplayName("Deve permitir adicionar peça com status RECEBIDA")
        void devePermitirAdicionarEmStatusRecebida() {
            os.setStatus(StatusOS.RECEBIDA);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));
            when(repository.save(any())).thenReturn(os);

            service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 1));

            assertThat(os.getItensPeca()).hasSize(1);
            verify(repository).save(os);
        }

        @Test
        @DisplayName("Deve permitir adicionar peça com status EM_DIAGNOSTICO")
        void devePermitirAdicionarEmStatusEmDiagnostico() {
            os.setStatus(StatusOS.EM_DIAGNOSTICO);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));
            when(repository.save(any())).thenReturn(os);

            service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 1));

            assertThat(os.getItensPeca()).hasSize(1);
            verify(repository).save(os);
        }

        @Test
        @DisplayName("Deve permitir adicionar peça com status AGUARDANDO_APROVACAO")
        void devePermitirAdicionarEmStatusAguardandoAprovacao() {
            os.setStatus(StatusOS.AGUARDANDO_APROVACAO);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));
            when(repository.save(any())).thenReturn(os);

            service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 1));

            assertThat(os.getItensPeca()).hasSize(1);
            verify(repository).save(os);
        }

        @Test
        @DisplayName("Deve permitir adicionar peça com status EM_EXECUCAO")
        void devePermitirAdicionarEmStatusEmExecucao() {
            os.setStatus(StatusOS.EM_EXECUCAO);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));
            when(repository.save(any())).thenReturn(os);

            service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 1));

            assertThat(os.getItensPeca()).hasSize(1);
            verify(repository).save(os);
        }

        @Test
        @DisplayName("Deve rejeitar adicionar em status FINALIZADA")
        void deveRejeitarAdicionarEmStatusFinalizada() {
            os.setStatus(StatusOS.FINALIZADA);
            when(repository.findById(osId)).thenReturn(Optional.of(os));

            assertThatThrownBy(() -> service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("status");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve rejeitar adicionar em status ENTREGUE")
        void deveRejeitarAdicionarEmStatusEntregue() {
            os.setStatus(StatusOS.ENTREGUE);
            when(repository.findById(osId)).thenReturn(Optional.of(os));

            assertThatThrownBy(() -> service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("status");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve rejeitar adicionar em status CANCELADA")
        void deveRejeitarAdicionarEmStatusCancelada() {
            os.setStatus(StatusOS.CANCELADA);
            when(repository.findById(osId)).thenReturn(Optional.of(os));

            assertThatThrownBy(() -> service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("status");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando OS não existe")
        void deveLancarExcecaoQuandoOSNaoExiste() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.adicionarPecaAOrdem(UUID.randomUUID(), new ItemPecaRequest(pecaId, 1)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("OS não encontrada");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando peça não existe")
        void deveLancarExcecaoQuandoPecaNaoExiste() {
            os.setStatus(StatusOS.RECEBIDA);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 1)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Peça não encontrada");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando peça está desativada")
        void deveLancarExcecaoQuandoPecaDesativada() {
            os.setStatus(StatusOS.RECEBIDA);
            peca.setAtivo(false);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));

            assertThatThrownBy(() -> service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("desativada");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando estoque insuficiente")
        void deveLancarExcecaoQuandoEstoqueInsuficiente() {
            os.setStatus(StatusOS.RECEBIDA);
            peca.setQuantidadeEstoque(2);
            when(repository.findById(osId)).thenReturn(Optional.of(os));
            when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));

            assertThatThrownBy(() -> service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 5)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Estoque insuficiente");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando tenta adicionar peça duplicada")
        void deveLancarExcecaoQuandoPecaDuplicada() {
            os.setStatus(StatusOS.RECEBIDA);
            ItemPeca itemExistente = new ItemPeca();
            itemExistente.setPeca(peca);
            itemExistente.setQuantidade(1);
            itemExistente.setPrecoUnitario(peca.getPreco());
            os.getItensPeca().add(itemExistente);

            when(repository.findById(osId)).thenReturn(Optional.of(os));

            assertThatThrownBy(() -> service.adicionarPecaAOrdem(osId, new ItemPecaRequest(pecaId, 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já foi adicionada");

            verify(repository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private void stubCriarBasico() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(repository.save(any())).thenReturn(os);
    }

    private void stubCriarComPeca() {
        stubCriarBasico();
        when(pecaRepository.findByIdParaAtualizacao(pecaId)).thenReturn(Optional.of(peca));
    }

    private OrdemDeServicoRequest requestSomenteServico() {
        return new OrdemDeServicoRequest(
                clienteId, veiculoId,
                "Barulho ao frear",
                List.of(new ItemServicoRequest(servicoId, null)),
                null
        );
    }

    private OrdemDeServicoRequest requestComPeca(int quantidade) {
        return new OrdemDeServicoRequest(
                clienteId, veiculoId,
                "Barulho ao frear",
                List.of(new ItemServicoRequest(servicoId, null)),
                List.of(new ItemPecaRequest(pecaId, quantidade))
        );
    }
}
