# Critical Flow Test Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Strengthen automated test coverage for the workshop's critical backend flows by adding targeted unit and controller tests for scheduling, payment, and purchase-order endpoints, then verify 80%+ coverage on the critical domains with fresh JaCoCo evidence.

**Architecture:** The implementation stays inside the current layered Spring Boot backend and follows existing test conventions: domain/entity tests in `src/test/java/.../model`, service tests in `.../service`, and HTTP contract tests in `.../controller` using `@WebMvcTest`. Work proceeds in TDD order, starting from the largest coverage gap (`Agendamento`) and then closing public endpoint gaps that support payment and inventory administration.

**Tech Stack:** Java 17, Spring Boot 3.2, JUnit 5, Mockito, MockMvc, AssertJ, JaCoCo, Maven Wrapper

---

## File Structure

**Create**
- `src/test/java/br/com/fiap/siase/model/AgendamentoEntityTest.java`
- `src/test/java/br/com/fiap/siase/service/AgendamentoServiceTest.java`
- `src/test/java/br/com/fiap/siase/controller/PagamentoControllerTest.java`
- `src/test/java/br/com/fiap/siase/controller/PedidoCompraControllerTest.java`
- `docs/test-coverage/critical-flow-coverage-2026-04-20.md`

**Modify only if a test reveals a real issue**
- `src/main/java/br/com/fiap/siase/model/Agendamento.java`
- `src/main/java/br/com/fiap/siase/service/AgendamentoService.java`
- `src/main/java/br/com/fiap/siase/controller/PagamentoController.java`
- `src/main/java/br/com/fiap/siase/controller/PedidoCompraController.java`

**Reference patterns**
- `src/test/java/br/com/fiap/siase/model/OrdemDeServicoEntityTest.java`
- `src/test/java/br/com/fiap/siase/service/PagamentoServiceTest.java`
- `src/test/java/br/com/fiap/siase/controller/OrdemDeServicoControllerTest.java`
- `target/site/jacoco/jacoco.csv`

### Task 1: Baseline Critical Coverage

**Files:**
- Read: `target/site/jacoco/jacoco.csv`
- Create: `docs/test-coverage/critical-flow-coverage-2026-04-20.md`

- [ ] **Step 1: Record the failing baseline expectation**

Create `docs/test-coverage/critical-flow-coverage-2026-04-20.md` with the initial table and leave the "final" column empty so the gap is explicit:

```md
# Critical Flow Coverage Evidence

## Baseline

| Class | Current signal |
| --- | --- |
| `Agendamento` | near 0% |
| `AgendamentoService` | near 0% |
| `PagamentoController` | 0% |
| `PedidoCompraController` | 0% |

## Target

- Reach at least 80% line coverage for the critical classes touched in this round.
- Preserve existing passing coverage on `OrdemDeServicoService`, `PagamentoService`, and `PecaService`.
```

- [ ] **Step 2: Run the current coverage report to verify the gap exists**

Run: `cmd /c "set JAVA_HOME=C:\Java\jdk-17.0.2&& set PATH=C:\Java\jdk-17.0.2\bin;%PATH%&& java -Dmaven.multiModuleProjectDirectory=%CD% -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain test"`

Expected:
- PASS on the current suite
- Existing `target/site/jacoco/jacoco.csv` still showing zero or near-zero coverage for the classes above

- [ ] **Step 3: Note the exact classes to attack first**

Append this bullet list to the evidence file:

```md
## Execution order

1. `AgendamentoEntityTest`
2. `AgendamentoServiceTest`
3. `PagamentoControllerTest`
4. `PedidoCompraControllerTest`
5. Full suite + JaCoCo evidence refresh
```

- [ ] **Step 4: Commit the baseline note**

```bash
git add docs/test-coverage/critical-flow-coverage-2026-04-20.md
git commit -m "docs: record critical flow coverage baseline"
```

### Task 2: Cover Agendamento Domain Transitions

**Files:**
- Create: `src/test/java/br/com/fiap/siase/model/AgendamentoEntityTest.java`
- Test target: `src/main/java/br/com/fiap/siase/model/Agendamento.java`
- Reference: `src/test/java/br/com/fiap/siase/model/OrdemDeServicoEntityTest.java`

- [ ] **Step 1: Write the failing entity tests**

Create `src/test/java/br/com/fiap/siase/model/AgendamentoEntityTest.java` with the initial red cases:

```java
package br.com.fiap.siase.model;

import br.com.fiap.siase.model.enums.StatusAgendamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agendamento - Regras de Domínio")
class AgendamentoEntityTest {

    @Nested
    @DisplayName("Confirmar")
    class Confirmar {

        @Test
        @DisplayName("Deve confirmar agendamento com status AGENDADO")
        void deveConfirmarAgendamento() {
            Agendamento agendamento = new Agendamento();

            agendamento.confirmar();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        }

        @Test
        @DisplayName("Deve rejeitar confirmação fora do status AGENDADO")
        void deveRejeitarConfirmacaoInvalida() {
            Agendamento agendamento = new Agendamento();
            agendamento.confirmar();

            assertThatThrownBy(agendamento::confirmar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AGENDADO");
        }
    }

    @Nested
    @DisplayName("Cancelar")
    class Cancelar {

        @Test
        @DisplayName("Deve cancelar agendamento ainda não realizado")
        void deveCancelarAgendamento() {
            Agendamento agendamento = new Agendamento();

            agendamento.cancelar();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        }

        @Test
        @DisplayName("Deve rejeitar cancelamento de agendamento realizado")
        void deveRejeitarCancelamentoDeRealizado() {
            Agendamento agendamento = new Agendamento();
            agendamento.setStatus(StatusAgendamento.REALIZADO);

            assertThatThrownBy(agendamento::cancelar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não pode ser cancelado");
        }
    }

    @Nested
    @DisplayName("Realizar")
    class Realizar {

        @Test
        @DisplayName("Deve realizar agendamento confirmado e vincular OS")
        void deveRealizarAgendamentoConfirmado() {
            Agendamento agendamento = new Agendamento();
            OrdemDeServico ordem = new OrdemDeServico();
            agendamento.confirmar();

            agendamento.realizar(ordem);

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.REALIZADO);
            assertThat(agendamento.getOrdemDeServico()).isSameAs(ordem);
        }

        @Test
        @DisplayName("Deve rejeitar realização fora do status CONFIRMADO")
        void deveRejeitarRealizacaoInvalida() {
            Agendamento agendamento = new Agendamento();

            assertThatThrownBy(() -> agendamento.realizar(new OrdemDeServico()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CONFIRMADOS");
        }
    }
}
```

- [ ] **Step 2: Run the new entity test and verify red**

Run: `cmd /c "set JAVA_HOME=C:\Java\jdk-17.0.2&& set PATH=C:\Java\jdk-17.0.2\bin;%PATH%&& java -Dmaven.multiModuleProjectDirectory=%CD% -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -Dtest=AgendamentoEntityTest test"`

Expected:
- If the class does not exist yet, compilation/test discovery fails
- If the class compiles, at least one assertion fails until the code matches expected behavior

- [ ] **Step 3: Write the minimal production fix only if needed**

If the tests reveal a real mismatch, keep `Agendamento.java` minimal and explicit:

```java
public void confirmar() {
    if (this.status != StatusAgendamento.AGENDADO) {
        throw new IllegalStateException("Apenas agendamentos com status AGENDADO podem ser confirmados.");
    }
    this.status = StatusAgendamento.CONFIRMADO;
}

public void cancelar() {
    if (this.status == StatusAgendamento.REALIZADO) {
        throw new IllegalStateException("Agendamento já realizado não pode ser cancelado.");
    }
    this.status = StatusAgendamento.CANCELADO;
}

public void realizar(OrdemDeServico os) {
    if (this.status != StatusAgendamento.CONFIRMADO) {
        throw new IllegalStateException("Apenas agendamentos CONFIRMADOS podem ser realizados.");
    }
    this.status = StatusAgendamento.REALIZADO;
    this.ordemDeServico = os;
}
```

- [ ] **Step 4: Run the entity test to verify green**

Run: `cmd /c "set JAVA_HOME=C:\Java\jdk-17.0.2&& set PATH=C:\Java\jdk-17.0.2\bin;%PATH%&& java -Dmaven.multiModuleProjectDirectory=%CD% -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -Dtest=AgendamentoEntityTest test"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/br/com/fiap/siase/model/AgendamentoEntityTest.java src/main/java/br/com/fiap/siase/model/Agendamento.java
git commit -m "test: cover agendamento domain transitions"
```

### Task 3: Cover Agendamento Service Flow

**Files:**
- Create: `src/test/java/br/com/fiap/siase/service/AgendamentoServiceTest.java`
- Test target: `src/main/java/br/com/fiap/siase/service/AgendamentoService.java`
- Reference: `src/test/java/br/com/fiap/siase/service/PagamentoServiceTest.java`

- [ ] **Step 1: Write the failing service tests**

Create `src/test/java/br/com/fiap/siase/service/AgendamentoServiceTest.java` with these scenarios:

```java
package br.com.fiap.siase.service;

import br.com.fiap.siase.dto.request.AgendamentoRequest;
import br.com.fiap.siase.dto.response.AgendamentoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.Agendamento;
import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.Veiculo;
import br.com.fiap.siase.model.enums.StatusAgendamento;
import br.com.fiap.siase.repository.AgendamentoRepository;
import br.com.fiap.siase.repository.ClienteRepository;
import br.com.fiap.siase.repository.VeiculoRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgendamentoService - Regras de Negócio")
class AgendamentoServiceTest {

    @Mock private AgendamentoRepository repository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private VeiculoRepository veiculoRepository;
    @Mock private EmailService emailService;

    @InjectMocks private AgendamentoService service;

    private UUID clienteId;
    private UUID veiculoId;
    private UUID agendamentoId;
    private Cliente cliente;
    private Veiculo veiculo;
    private Agendamento agendamento;
    private AgendamentoRequest request;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
        agendamentoId = UUID.randomUUID();

        cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", clienteId);
        cliente.setNome("Maria Oliveira");
        cliente.setEmail("maria@email.com");

        veiculo = new Veiculo();
        ReflectionTestUtils.setField(veiculo, "id", veiculoId);
        veiculo.setCliente(cliente);
        veiculo.setMarca("Toyota");
        veiculo.setModelo("Corolla");
        veiculo.setPlaca("ABC1234");

        agendamento = new Agendamento();
        ReflectionTestUtils.setField(agendamento, "id", agendamentoId);
        agendamento.setCliente(cliente);
        agendamento.setVeiculo(veiculo);
        agendamento.setDataHora(LocalDateTime.now().plusDays(1));
        agendamento.setDescricaoServicos("Revisão e alinhamento");

        request = new AgendamentoRequest(
                clienteId,
                veiculoId,
                LocalDateTime.now().plusDays(2),
                "Revisão e alinhamento"
        );
    }

    @Nested
    @DisplayName("Criar")
    class Criar {
        @Test
        @DisplayName("Deve criar agendamento e enviar confirmação")
        void deveCriarAgendamentoComSucesso() {
            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
            when(repository.save(any(Agendamento.class))).thenReturn(agendamento);

            AgendamentoResponse response = service.criar(request);

            assertThat(response.clienteId()).isEqualTo(clienteId);
            assertThat(response.veiculoId()).isEqualTo(veiculoId);
            verify(emailService).enviarConfirmacaoAgendamento(anyString(), eq("Maria Oliveira"), anyString(), contains("ABC1234"));
        }

        @Test
        @DisplayName("Deve rejeitar veículo de outro cliente")
        void deveRejeitarVeiculoDeOutroCliente() {
            Cliente outroCliente = new Cliente();
            ReflectionTestUtils.setField(outroCliente, "id", UUID.randomUUID());
            veiculo.setCliente(outroCliente);

            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));

            assertThatThrownBy(() -> service.criar(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não pertence ao cliente");
        }
    }

    @Nested
    @DisplayName("Consultas e transições")
    class ConsultasETransicoes {
        @Test
        @DisplayName("Deve listar todos os agendamentos")
        void deveListarTodos() {
            when(repository.findAll()).thenReturn(List.of(agendamento));

            assertThat(service.listar()).hasSize(1);
        }

        @Test
        @DisplayName("Deve listar por status")
        void deveListarPorStatus() {
            when(repository.findByStatus(StatusAgendamento.AGENDADO)).thenReturn(List.of(agendamento));

            assertThat(service.listarPorStatus(StatusAgendamento.AGENDADO)).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar por cliente")
        void deveListarPorCliente() {
            when(repository.findByClienteId(clienteId)).thenReturn(List.of(agendamento));

            assertThat(service.listarPorCliente(clienteId)).hasSize(1);
        }

        @Test
        @DisplayName("Deve confirmar agendamento existente")
        void deveConfirmarAgendamento() {
            when(repository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
            when(repository.save(any(Agendamento.class))).thenReturn(agendamento);

            AgendamentoResponse response = service.confirmar(agendamentoId);

            assertThat(response.status()).isEqualTo("CONFIRMADO");
        }

        @Test
        @DisplayName("Deve cancelar agendamento existente")
        void deveCancelarAgendamento() {
            when(repository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
            when(repository.save(any(Agendamento.class))).thenReturn(agendamento);

            AgendamentoResponse response = service.cancelar(agendamentoId);

            assertThat(response.status()).isEqualTo("CANCELADO");
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException ao buscar id inexistente")
        void deveLancarQuandoIdNaoExiste() {
            when(repository.findById(agendamentoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(agendamentoId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Agendamento não encontrado");
        }
    }
}
```

- [ ] **Step 2: Run the service test and verify red**

Run: `cmd /c "set JAVA_HOME=C:\Java\jdk-17.0.2&& set PATH=C:\Java\jdk-17.0.2\bin;%PATH%&& java -Dmaven.multiModuleProjectDirectory=%CD% -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -Dtest=AgendamentoServiceTest test"`

Expected:
- FAIL on missing test class first
- Then FAIL only if current service behavior differs from the scenarios above

- [ ] **Step 3: Apply the minimal production adjustment only if tests reveal one**

Keep `AgendamentoService.java` aligned with the current behavior. If a bug is exposed, the production fix should stay in the current shape:

```java
if (!veiculo.getCliente().getId().equals(cliente.getId())) {
    throw new BusinessException("O veículo informado não pertence ao cliente.");
}

var salvo = repository.save(agendamento);

String emailCliente = cliente.getEmail() != null ? cliente.getEmail() : "sem-email@siase.com";
emailService.enviarConfirmacaoAgendamento(
        emailCliente,
        cliente.getNome(),
        request.dataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
        veiculo.getMarca() + " " + veiculo.getModelo() + " - " + veiculo.getPlaca()
);
```

- [ ] **Step 4: Run the service test to verify green**

Run: `cmd /c "set JAVA_HOME=C:\Java\jdk-17.0.2&& set PATH=C:\Java\jdk-17.0.2\bin;%PATH%&& java -Dmaven.multiModuleProjectDirectory=%CD% -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -Dtest=AgendamentoServiceTest test"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/br/com/fiap/siase/service/AgendamentoServiceTest.java src/main/java/br/com/fiap/siase/service/AgendamentoService.java
git commit -m "test: cover agendamento service flow"
```

### Task 4: Cover Pagamento Controller Contracts

**Files:**
- Create: `src/test/java/br/com/fiap/siase/controller/PagamentoControllerTest.java`
- Test target: `src/main/java/br/com/fiap/siase/controller/PagamentoController.java`
- Reference: `src/test/java/br/com/fiap/siase/controller/OrdemDeServicoControllerTest.java`

- [ ] **Step 1: Write the failing controller tests**

Create `src/test/java/br/com/fiap/siase/controller/PagamentoControllerTest.java`:

```java
package br.com.fiap.siase.controller;

import br.com.fiap.siase.config.SecurityConfig;
import br.com.fiap.siase.dto.response.PagamentoResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.GlobalExceptionHandler;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.enums.FormaPagamento;
import br.com.fiap.siase.model.enums.StatusPagamento;
import br.com.fiap.siase.security.JwtService;
import br.com.fiap.siase.security.UserDetailsServiceImpl;
import br.com.fiap.siase.service.PagamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagamentoController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
@DisplayName("PagamentoController - Endpoints REST")
class PagamentoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PagamentoService service;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private UUID osId;
    private UUID pagamentoId;
    private PagamentoResponse response;

    @BeforeEach
    void setUp() {
        osId = UUID.randomUUID();
        pagamentoId = UUID.randomUUID();
        response = new PagamentoResponse(
                pagamentoId,
                osId,
                "OS-001",
                "Maria Oliveira",
                FormaPagamento.PIX.name(),
                FormaPagamento.PIX.getDescricao(),
                new BigDecimal("500.00"),
                StatusPagamento.PENDENTE.name(),
                StatusPagamento.PENDENTE.getDescricao(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /ordens/{osId}/pagamento")
    class Registrar {
        @Test
        @DisplayName("Deve retornar 201 com Location")
        void deveRegistrarPagamento() throws Exception {
            when(service.registrar(any(), any())).thenReturn(response);

            mockMvc.perform(post("/ordens/{osId}/pagamento", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "formaPagamento": "PIX",
                                      "valor": 500.00
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(pagamentoId.toString())))
                    .andExpect(jsonPath("$.ordemDeServicoId", is(osId.toString())))
                    .andExpect(jsonPath("$.formaPagamento", is("PIX")));
        }

        @Test
        @DisplayName("Deve retornar 400 para payload inválido")
        void deveRetornar400ParaPayloadInvalido() throws Exception {
            mockMvc.perform(post("/ordens/{osId}/pagamento", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "valor": 0
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET/PATCH pagamento")
    class LeituraETransicoes {
        @Test
        @DisplayName("Deve buscar pagamento por OS")
        void deveBuscarPorOS() throws Exception {
            when(service.buscarPorOS(osId)).thenReturn(response);

            mockMvc.perform(get("/ordens/{osId}/pagamento", osId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clienteNome", is("Maria Oliveira")));
        }

        @Test
        @DisplayName("Deve confirmar pagamento")
        void deveConfirmarPagamento() throws Exception {
            PagamentoResponse confirmado = new PagamentoResponse(
                    pagamentoId, osId, "OS-001", "Maria Oliveira", "PIX", "Pix",
                    new BigDecimal("500.00"), "PAGO", "Pago",
                    LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
            );
            when(service.confirmar(pagamentoId)).thenReturn(confirmado);

            mockMvc.perform(patch("/pagamentos/{id}/confirmar", pagamentoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PAGO")));
        }

        @Test
        @DisplayName("Deve cancelar pagamento")
        void deveCancelarPagamento() throws Exception {
            PagamentoResponse cancelado = new PagamentoResponse(
                    pagamentoId, osId, "OS-001", "Maria Oliveira", "PIX", "Pix",
                    new BigDecimal("500.00"), "CANCELADO", "Cancelado",
                    null, LocalDateTime.now(), LocalDateTime.now()
            );
            when(service.cancelar(pagamentoId)).thenReturn(cancelado);

            mockMvc.perform(patch("/pagamentos/{id}/cancelar", pagamentoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELADO")));
        }

        @Test
        @DisplayName("Deve retornar 404 quando pagamento não existe")
        void deveRetornar404QuandoNaoExiste() throws Exception {
            when(service.buscarPorOS(osId)).thenThrow(new ResourceNotFoundException("Pagamento não encontrado"));

            mockMvc.perform(get("/ordens/{osId}/pagamento", osId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 422 em regra de negócio")
        void deveRetornar422EmRegraDeNegocio() throws Exception {
            when(service.confirmar(pagamentoId)).thenThrow(new BusinessException("Somente pagamentos pendentes podem ser confirmados."));

            mockMvc.perform(patch("/pagamentos/{id}/confirmar", pagamentoId))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("pendentes")));
        }
    }
}
```

- [ ] **Step 2: Run the controller test and verify red**

Run: `cmd /c "set JAVA_HOME=C:\Java\jdk-17.0.2&& set PATH=C:\Java\jdk-17.0.2\bin;%PATH%&& java -Dmaven.multiModuleProjectDirectory=%CD% -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -Dtest=PagamentoControllerTest test"`

Expected:
- FAIL before the class exists
- Then PASS or expose contract mismatches to correct minimally

- [ ] **Step 3: Adjust controller behavior only if a mismatch is revealed**

The controller should remain a thin adapter:

```java
@PostMapping("/ordens/{osId}/pagamento")
public ResponseEntity<PagamentoResponse> registrar(@PathVariable UUID osId, @Valid @RequestBody PagamentoRequest request) {
    PagamentoResponse response = service.registrar(osId, request);
    var location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/pagamentos/{id}")
            .buildAndExpand(response.id())
            .toUri();
    return ResponseEntity.created(location).body(response);
}
```

- [ ] **Step 4: Run the controller test to verify green**

Run: `cmd /c "set JAVA_HOME=C:\Java\jdk-17.0.2&& set PATH=C:\Java\jdk-17.0.2\bin;%PATH%&& java -Dmaven.multiModuleProjectDirectory=%CD% -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -Dtest=PagamentoControllerTest test"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/br/com/fiap/siase/controller/PagamentoControllerTest.java src/main/java/br/com/fiap/siase/controller/PagamentoController.java
git commit -m "test: cover pagamento controller endpoints"
```

### Task 5: Cover PedidoCompra Controller Contracts

**Files:**
- Create: `src/test/java/br/com/fiap/siase/controller/PedidoCompraControllerTest.java`
- Test target: `src/main/java/br/com/fiap/siase/controller/PedidoCompraController.java`
- Reference: `src/test/java/br/com/fiap/siase/controller/OrdemDeServicoControllerTest.java`

- [ ] **Step 1: Write the failing controller tests**

Create `src/test/java/br/com/fiap/siase/controller/PedidoCompraControllerTest.java`:

```java
package br.com.fiap.siase.controller;

import br.com.fiap.siase.config.SecurityConfig;
import br.com.fiap.siase.dto.response.PedidoCompraResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.GlobalExceptionHandler;
import br.com.fiap.siase.exception.ResourceNotFoundException;
import br.com.fiap.siase.model.enums.StatusPedidoCompra;
import br.com.fiap.siase.security.JwtService;
import br.com.fiap.siase.security.UserDetailsServiceImpl;
import br.com.fiap.siase.service.PedidoCompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PedidoCompraController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
@DisplayName("PedidoCompraController - Endpoints REST")
class PedidoCompraControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PedidoCompraService service;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private UUID pedidoId;
    private UUID pecaId;
    private PedidoCompraResponse response;

    @BeforeEach
    void setUp() {
        pedidoId = UUID.randomUUID();
        pecaId = UUID.randomUUID();
        response = new PedidoCompraResponse(
                pedidoId,
                pecaId,
                "PEC-001",
                "Filtro de Óleo",
                5,
                0,
                StatusPedidoCompra.SOLICITADO.name(),
                StatusPedidoCompra.SOLICITADO.getDescricao(),
                "Compra urgente",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /pedidos-compra")
    class Criar {
        @Test
        @DisplayName("Deve retornar 201 ao criar pedido")
        void deveCriarPedido() throws Exception {
            when(service.criar(any())).thenReturn(response);

            mockMvc.perform(post("/pedidos-compra")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "pecaId": "%s",
                                      "quantidadeSolicitada": 5,
                                      "observacoes": "Compra urgente"
                                    }
                                    """.formatted(pecaId)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(pedidoId.toString())))
                    .andExpect(jsonPath("$.pecaCodigo", is("PEC-001")));
        }

        @Test
        @DisplayName("Deve retornar 400 para payload inválido")
        void deveRetornar400ParaPayloadInvalido() throws Exception {
            mockMvc.perform(post("/pedidos-compra")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "quantidadeSolicitada": 0
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET e PATCH /pedidos-compra")
    class LeituraETransicoes {
        @Test
        @DisplayName("Deve listar todos sem filtro")
        void deveListarTodos() throws Exception {
            when(service.listar()).thenReturn(List.of(response));

            mockMvc.perform(get("/pedidos-compra"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("SOLICITADO")));

            verify(service).listar();
            verify(service, never()).listarPorStatus(any());
        }

        @Test
        @DisplayName("Deve listar por status quando filtro existir")
        void deveListarPorStatus() throws Exception {
            when(service.listarPorStatus(StatusPedidoCompra.SOLICITADO)).thenReturn(List.of(response));

            mockMvc.perform(get("/pedidos-compra").param("status", "SOLICITADO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("Deve buscar pedido por id")
        void deveBuscarPorId() throws Exception {
            when(service.buscarPorId(pedidoId)).thenReturn(response);

            mockMvc.perform(get("/pedidos-compra/{id}", pedidoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(pedidoId.toString())));
        }

        @Test
        @DisplayName("Deve aprovar pedido")
        void deveAprovarPedido() throws Exception {
            PedidoCompraResponse aprovado = new PedidoCompraResponse(
                    pedidoId, pecaId, "PEC-001", "Filtro de Óleo", 5, 0,
                    "APROVADO", "Aprovado", "Compra urgente", LocalDateTime.now(), LocalDateTime.now()
            );
            when(service.aprovar(pedidoId)).thenReturn(aprovado);

            mockMvc.perform(patch("/pedidos-compra/{id}/aprovar", pedidoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("APROVADO")));
        }

        @Test
        @DisplayName("Deve receber pedido com quantidade válida")
        void deveReceberPedido() throws Exception {
            PedidoCompraResponse recebido = new PedidoCompraResponse(
                    pedidoId, pecaId, "PEC-001", "Filtro de Óleo", 5, 5,
                    "RECEBIDO", "Recebido", "Compra urgente", LocalDateTime.now(), LocalDateTime.now()
            );
            when(service.receber(pedidoId, 5)).thenReturn(recebido);

            mockMvc.perform(patch("/pedidos-compra/{id}/receber", pedidoId).param("quantidade", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantidadeRecebida", is(5)));
        }

        @Test
        @DisplayName("Deve retornar 400 para quantidade inválida no recebimento")
        void deveRetornar400ParaQuantidadeInvalida() throws Exception {
            mockMvc.perform(patch("/pedidos-compra/{id}/receber", pedidoId).param("quantidade", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve cancelar pedido")
        void deveCancelarPedido() throws Exception {
            PedidoCompraResponse cancelado = new PedidoCompraResponse(
                    pedidoId, pecaId, "PEC-001", "Filtro de Óleo", 5, 0,
                    "CANCELADO", "Cancelado", "Compra urgente", LocalDateTime.now(), LocalDateTime.now()
            );
            when(service.cancelar(pedidoId)).thenReturn(cancelado);

            mockMvc.perform(patch("/pedidos-compra/{id}/cancelar", pedidoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELADO")));
        }

        @Test
        @DisplayName("Deve retornar 404 quando pedido não existe")
        void deveRetornar404QuandoNaoExiste() throws Exception {
            when(service.buscarPorId(pedidoId)).thenThrow(new ResourceNotFoundException("Pedido não encontrado"));

            mockMvc.perform(get("/pedidos-compra/{id}", pedidoId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 422 em regra de negócio")
        void deveRetornar422EmRegraDeNegocio() throws Exception {
            when(service.aprovar(pedidoId)).thenThrow(new BusinessException("Pedido já aprovado."));

            mockMvc.perform(patch("/pedidos-compra/{id}/aprovar", pedidoId))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("aprovado")));
        }
    }
}
```

- [ ] **Step 2: Run the controller test and verify red**

Run: `cmd /c "set JAVA_HOME=C:\Java\jdk-17.0.2&& set PATH=C:\Java\jdk-17.0.2\bin;%PATH%&& java -Dmaven.multiModuleProjectDirectory=%CD% -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -Dtest=PedidoCompraControllerTest test"`

Expected:
- FAIL before the class exists
- Then FAIL only on real contract mismatches

- [ ] **Step 3: Adjust controller behavior only if a mismatch is revealed**

Keep `PedidoCompraController.java` as a thin adapter:

```java
@GetMapping
public ResponseEntity<List<PedidoCompraResponse>> listar(@RequestParam(required = false) StatusPedidoCompra status) {
    List<PedidoCompraResponse> lista = status != null ? service.listarPorStatus(status) : service.listar();
    return ResponseEntity.ok(lista);
}
```

- [ ] **Step 4: Run the controller test to verify green**

Run: `cmd /c "set JAVA_HOME=C:\Java\jdk-17.0.2&& set PATH=C:\Java\jdk-17.0.2\bin;%PATH%&& java -Dmaven.multiModuleProjectDirectory=%CD% -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -Dtest=PedidoCompraControllerTest test"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/br/com/fiap/siase/controller/PedidoCompraControllerTest.java src/main/java/br/com/fiap/siase/controller/PedidoCompraController.java
git commit -m "test: cover pedido compra controller endpoints"
```

### Task 6: Full Verification and Coverage Evidence

**Files:**
- Modify: `docs/test-coverage/critical-flow-coverage-2026-04-20.md`
- Read: `target/site/jacoco/jacoco.csv`

- [ ] **Step 1: Run the focused regression set**

Run:

```bash
cmd /c "set JAVA_HOME=C:\Java\jdk-17.0.2&& set PATH=C:\Java\jdk-17.0.2\bin;%PATH%&& java -Dmaven.multiModuleProjectDirectory=%CD% -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -Dtest=AgendamentoEntityTest,AgendamentoServiceTest,PagamentoControllerTest,PedidoCompraControllerTest test"
```

Expected: PASS

- [ ] **Step 2: Run the full backend suite with fresh JaCoCo data**

Run:

```bash
cmd /c "set JAVA_HOME=C:\Java\jdk-17.0.2&& set PATH=C:\Java\jdk-17.0.2\bin;%PATH%&& java -Dmaven.multiModuleProjectDirectory=%CD% -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain test"
```

Expected:
- PASS
- `target/site/jacoco/jacoco.csv` refreshed

- [ ] **Step 3: Update the evidence document with final results**

Replace the baseline note with the final measured table:

```md
## Final

| Class | Expected result after implementation |
| --- | --- |
| `Agendamento` | >= 80% line coverage |
| `AgendamentoService` | >= 80% line coverage |
| `PagamentoController` | >= 80% line coverage |
| `PedidoCompraController` | >= 80% line coverage |

## Verification commands

- Focused: `-Dtest=AgendamentoEntityTest,AgendamentoServiceTest,PagamentoControllerTest,PedidoCompraControllerTest test`
- Full suite: `test`

## Notes

- Preserve all previously passing integration and service tests.
- If any critical class remains below 80%, add one more TDD cycle before closing the task.
```

- [ ] **Step 4: Re-run coverage if any critical class is still under target**

Decision rule:
- If any of the four target classes are below 80% line coverage, add the smallest missing red-green test and rerun Step 1 and Step 2.
- If all four are at or above 80%, proceed immediately.

- [ ] **Step 5: Commit**

```bash
git add docs/test-coverage/critical-flow-coverage-2026-04-20.md src/test/java/br/com/fiap/siase/model/AgendamentoEntityTest.java src/test/java/br/com/fiap/siase/service/AgendamentoServiceTest.java src/test/java/br/com/fiap/siase/controller/PagamentoControllerTest.java src/test/java/br/com/fiap/siase/controller/PedidoCompraControllerTest.java
git commit -m "test: verify critical flow coverage targets"
```
