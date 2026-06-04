# Design: Refatoração para Clean Architecture

**Data:** 2026-05-31  
**Branch:** v2.0.0  
**Escopo:** Corrigir todas as violações de Clean Architecture identificadas na code review

---

## Contexto

O projeto `siase` é uma aplicação Spring Boot com estrutura multi-módulo Maven:
- `siase-domain` — modelos, ports, enums, exceções, validações
- `siase-application` — use cases, DTOs
- `siase-infrastructure` — persistence (JPA), web (REST controllers), security (JWT)

A code review identificou que as entidades de domínio são também entidades JPA, o controller de Ordens de Serviço contém lógica de negócio, use cases não possuem interfaces de porta de entrada, e existem bugs funcionais nos use cases.

---

## Objetivo

Refatorar o projeto para conformidade com Clean Architecture:
1. Domínio 100% livre de dependências de framework
2. Use cases com interfaces de porta de entrada (DIP no lado primário)
3. Entidades JPA separadas das entidades de domínio com mappers MapStruct
4. Controllers delegando toda lógica a use cases
5. Correção de bugs funcionais identificados

---

## Seção 1: Dependências entre módulos

```
siase-domain
  └── dependências: Lombok (POJO helpers — @Getter, @Builder)
  └── REMOVE: jakarta.persistence-api, jakarta.validation

siase-application
  └── depende de: siase-domain
  └── dependências: Lombok, jakarta.validation (DTOs de input)

siase-infrastructure
  └── depende de: siase-application, siase-domain
  └── dependências: Spring Boot, Spring Data JPA, MapStruct, JWT, Flyway, etc.
```

---

## Seção 2: Camada de Domínio

### Entidades (POJOs puros)

Todas as classes em `siase-domain/model/` tornam-se POJOs sem anotações de framework:

- Remover `@Entity`, `@Table`, `@Column`, `@ManyToOne`, `@OneToMany`, `@JoinColumn`, `@Enumerated`, `@Embedded`, `@EmbeddedId` de todas as classes
- Remover `jakarta.persistence.*` e `jakarta.validation.*` de todos os imports
- `BaseEntity` é deletado — cada entidade de domínio declara `id`, `criadoEm`, `atualizadoEm` como campos simples
- Usar `@Getter @Builder` do Lombok; remover `@Setter` e `@NoArgsConstructor` (encapsulamento explícito via construtor)
- Manter toda lógica de negócio: `avancarStatus()`, `cancelar()`, `recalcularTotais()`, etc.

### Regras de negócio migradas para o domínio

- `gerarNumero()` (formato `OS-YYYYMMDD-XXXXXX`): movido de `CriarOrdemServicoUC` para `OrdemDeServico` como factory method estático ou parte do construtor
- FSM de transições de `StatusOS`: consolidada em `OrdemDeServico.avancarStatus()` e `OrdemDeServico.podeAvancarPara(StatusOS)` — o método privado `ehAvancar()` do `AtualizarStatusViaWebhookUC` é removido

### Ports (interfaces de repositório — sem alterações de comportamento)

As interfaces em `siase-domain/port/` permanecem. Remover qualquer import de JPA caso existam. Adicionar ao `OrdemServicoRepositoryPort`:
```java
Optional<Double> calcularTempoMedioExecucaoMinutos();
```

---

## Seção 3: Camada de Aplicação

### Input Port Interfaces

Novo pacote `siase-application/usecase/port/` com uma interface por use case:

| Interface | Método principal |
|---|---|
| `CriarOrdemServicoUCPort` | `execute(OrdemDeServicoRequest)` → `OrdemDeServicoResponse` |
| `AprovarOrcamentoUCPort` | `execute(UUID, PagamentoRequest)` → `OrdemDeServicoResponse` |
| `AtualizarStatusViaWebhookUCPort` | `execute(AtualizarStatusWebhookRequest, String token)` |
| `AvancarStatusUCPort` | `execute(UUID osId)` → `OrdemDeServicoResponse` |
| `CancelarOrdemUCPort` | `execute(UUID osId, String motivo)` → `OrdemDeServicoResponse` |
| `AdicionarPecaUCPort` | `execute(UUID osId, ItemPecaRequest)` → `OrdemDeServicoResponse` |
| `AdicionarServicoUCPort` | `execute(UUID osId, ItemServicoRequest)` → `OrdemDeServicoResponse` |
| `ConsultarTempoMedioUCPort` | `execute()` → `Double` |
| `ConsultarStatusOSUCPort` | `execute(UUID)` → `StatusOS` |
| `ListarOrdensServicoUCPort` | `execute()` → `List<OrdemDeServicoResponse>` |

Cada use case existente passa a implementar sua interface correspondente.

### Novos Use Cases

Extraídos do `OrdemServicoController`:

**`AvancarStatusUC`**
- Busca OS via `ordemServicoRepository`
- Chama `os.avancarStatus()` (delegando FSM ao domínio)
- Persiste
- Envia notificação correta por `StatusOS` (ver correção de bug abaixo)

**`CancelarOrdemUC`**
- Busca OS, chama `os.cancelar(motivo)`, persiste

**`AdicionarPecaUC`**
- Valida OS em status permitido
- Valida peça existe e tem estoque (via `PecaRepositoryPort`)
- Guard de duplicidade
- Reserva estoque
- Adiciona item, `os.recalcularTotais()`, persiste

**`AdicionarServicoUC`**
- Valida OS em status permitido
- Valida serviço ativo (via `ServicoRepositoryPort`)
- Guard de duplicidade
- Adiciona item, `os.recalcularTotais()`, persiste

**`ConsultarTempoMedioUC`**
- Chama `ordemServicoRepository.calcularTempoMedioExecucaoMinutos()`

### Correções em Use Cases Existentes

**`AtualizarStatusViaWebhookUC`**
- `WEBHOOK_TOKEN` removido do `System.getenv()` — recebido via construtor (`String webhookToken`) e injetado pela infra via `@Value("${webhook.token}")`
- Bug corrigido: `notificarCliente()` ramifica por `StatusOS` para enviar o template correto de e-mail
- Método `ehAvancar()` (FSM duplicada) removido — delega para `OrdemDeServico.podeAvancarPara()`

**`CriarOrdemServicoUC`**
- `gerarNumero()` removido — chama `OrdemDeServico.gerarNumero()` do domínio

---

## Seção 4: Camada de Infraestrutura

### Entidades JPA (novo pacote `infrastructure/persistence/entity/`)

Uma classe JPA por aggregate, com toda a configuração de mapeamento:

```
BaseJpaEntity.java          @MappedSuperclass — @Id, @PrePersist, @PreUpdate
OrdemDeServicoEntity.java   @Entity @Table(name="ordens_de_servico")
ClienteEntity.java
VeiculoEntity.java
PecaEntity.java
ServicoEntity.java
ItemPecaEntity.java
ItemServicoEntity.java
PagamentoEntity.java
PedidoCompraEntity.java
AgendamentoEntity.java
UsuarioEntity.java
ServicoInsumoEntity.java
ServicoInsumoEntityId.java  (@EmbeddedId)
```

As classes JPA Entity recebem `@Getter @Setter @NoArgsConstructor` (Lombok) — o open setter é aceitável aqui pois é detalhe de persistência, não domínio.

### JPA Repositories

Todos os `*JpaRepository` atualizados para usar `*Entity` em vez das classes de domínio:
```java
// ANTES
interface OrdemServicoJpaRepository extends JpaRepository<OrdemDeServico, UUID>
// DEPOIS
interface OrdemServicoJpaRepository extends JpaRepository<OrdemDeServicoEntity, UUID>
```

`OrdemServicoJpaRepository` ganha o método:
```java
@Query("SELECT AVG(...) FROM OrdemDeServicoEntity ...")
Optional<Double> calcularTempoMedioExecucaoMinutos();
```

### MapStruct Mappers (`infrastructure/persistence/mapper/`)

```
OrdemDeServicoMapper.java
ClienteMapper.java
VeiculoMapper.java
PecaMapper.java
ServicoMapper.java
ItemPecaMapper.java
ItemServicoMapper.java
PagamentoMapper.java
PedidoCompraMapper.java
AgendamentoMapper.java
UsuarioMapper.java
```

Cada mapper expõe `toDomain(Entity)` e `toEntity(Domain)`. Relacionamentos são mapeados recursivamente via `@Mapper(uses = {...})`.

### Repository Adapters

Cada `*RepositoryAdapter` usa seu mapper:
```java
@Override
public OrdemDeServico save(OrdemDeServico os) {
    OrdemDeServicoEntity entity = mapper.toEntity(os);
    OrdemDeServicoEntity saved = jpaRepository.save(entity);
    return mapper.toDomain(saved);
}
```

### Controllers (web)

**`OrdemServicoController`** reestruturado:
- Remove `OrdemServicoJpaRepository` (injeção direta de JPA — removida)
- Remove `OrdemServicoRepositoryPort`, `ClienteRepositoryPort`, `VeiculoRepositoryPort`, `PecaRepositoryPort`, `ServicoRepositoryPort` (repositórios diretos — removidos)
- Injeta apenas use case interfaces: `AvancarStatusUCPort`, `CancelarOrdemUCPort`, `AdicionarPecaUCPort`, `AdicionarServicoUCPort`, `ConsultarTempoMedioUCPort`, e os já existentes

**`WebhookController`** passa o token recebido no header ao use case em vez de comparar dentro do use case.

### Configuração

`application.yml`: adicionar:
```yaml
webhook:
  token: ${WEBHOOK_TOKEN:changeme}
```

Spring `@Configuration` do use case `AtualizarStatusViaWebhookUC` injeta `@Value("${webhook.token}")`.

---

## Bugs corrigidos

| Bug | Localização | Correção |
|---|---|---|
| E-mail errado para todos os status | `AtualizarStatusViaWebhookUC:66` | Ramificar por `StatusOS` em `notificarCliente()` |
| `System.getenv()` no use case | `AtualizarStatusViaWebhookUC:12` | Injetar token via construtor |
| FSM duplicada | `AtualizarStatusViaWebhookUC:51` | Remover `ehAvancar()`, delegar ao domínio |

---

## O que NÃO muda

- Schema do banco (Flyway migrations intactas)
- Endpoints REST (paths, métodos HTTP, request/response shapes)
- Lógica de segurança JWT
- Testes existentes (serão atualizados para mockar interfaces em vez de classes concretas)

---

## Escopo fora deste design

- Novos endpoints ou features
- Mudanças no schema do banco
- CI/CD / Docker / Kubernetes
