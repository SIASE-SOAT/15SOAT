# SIASE - Sistema Integrado de Atendimento e Execução de Serviços

> Pós Tech - Software Architecture | FIAP | **Fase 4 — Microsserviços, Saga Pattern e Integração Mercado Pago**

> **README v1 (Fase 3):** [README-v1.md](README-v1.md)

## Descrição da Solução

O SIASE é um sistema de gestão de oficina mecânica evoluído ao longo de 4 fases:

- **Fase 1:** MVP com DDD, APIs REST, autenticação JWT, controle de estoque e fluxo completo de OS
- **Fase 2:** Refatoração para Arquitetura Hexagonal, Kubernetes, Terraform e CI/CD automatizado
- **Fase 3:** API Gateway com Lambda Authorizer, observabilidade completa (Prometheus + Grafana + Loki), EKS gerenciado
- **Fase 4:** Migração para microsserviços, Saga Coreografado via AWS SQS, DynamoDB, integração Mercado Pago e BDD

---

## Arquitetura — Fase 4

### Visão Geral

```
                          Internet
                              │
                              ▼
                   ┌─────────────────────┐
                   │   AWS API Gateway   │
                   │     HTTP API        │
                   └──────────┬──────────┘
                              │
              ┌───────────────┼───────────────────┐
              │               │                   │
              ▼               ▼                   │
    POST /auth/token    ANY /{proxy+}    Lambda Authorizer
              │         (protegida)      verifica JWT HS256
              ▼               │
    Lambda Token          ALB (EKS)
    valida CPF                │
    emite JWT                 ▼
                   ┌──────────────────────────────────────────┐
                   │              EKS (AWS)                   │
                   │                                          │
                   │  ┌──────────────────────────────────┐    │
                   │  │      siase-os-service :8081      │    │
                   │  │  OS, status, aprovação           │    │
                   │  │  RDS PostgreSQL + DynamoDB       │    │
                   │  └──────────────┬───────────────────┘    │
                   │                 │ SQS (billing-queue)     │
                   │  ┌──────────────▼───────────────────┐    │
                   │  │   siase-billing-service :8082    │    │
                   │  │  Orçamentos + Mercado Pago       │    │
                   │  │  RDS PostgreSQL                  │    │
                   │  └──────────────┬───────────────────┘    │
                   │                 │ SQS (execution-queue)   │
                   │  ┌──────────────▼───────────────────┐    │
                   │  │  siase-execution-service :8083   │    │
                   │  │  Fila e execução de serviços     │    │
                   │  │  RDS PostgreSQL + DynamoDB       │    │
                   │  └──────────────────────────────────┘    │
                   │                                          │
                   │  monitoring namespace:                   │
                   │  Prometheus · Grafana · Loki · Alloy     │
                   └──────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────────┐
              │               │                   │
              ▼               ▼                   ▼
    RDS PostgreSQL        DynamoDB            AWS SQS
    (por serviço)      (histórico eventos)  (4 filas)
```

### Saga Pattern — Coreografado via SQS

```
OS criada ──► [SQS billing-queue] ──► Billing gera orçamento
                                              │
                                     [SQS os-updates] ──► OS: AGUARDANDO_APROVACAO
                                              │
Cliente aprova ──► [SQS execution-queue] ──► Execution inicia
                                              │
                                     [SQS os-updates] ──► OS: EM_EXECUCAO
                                              │
Execução finaliza ──► [SQS os-updates] ──► OS: FINALIZADA
                                              │
Pagamento Mercado Pago ──► [SQS os-updates] ──► OS: ENTREGUE

Rollback: OS cancelada ──► [SQS billing + execution] ──► compensação
```

---

## Repositórios (Fase 4)

| Repositório | Porta | Descrição |
|---|---|---|
| `siase-os-service` | 8081 | Ciclo de vida das OS + DynamoDB histórico |
| `siase-billing-service` | 8082 | Orçamentos + Mercado Pago |
| `siase-execution-service` | 8083 | Fila e execução + DynamoDB estado |
| `siase-auth-lambda` | — | Lambda Authorizer + Token (Fase 3, mantido) |
| `siase-infra-k8s` | — | EKS, VPC, observabilidade (Fase 3, mantido) |
| `siase-infra-database` | — | RDS PostgreSQL (Fase 3, mantido) |

---

## Filas SQS

| Fila | Produtor | Consumidor |
|---|---|---|
| `siase-billing-queue` | OS Service | Billing Service |
| `siase-execution-queue` | OS Service, Billing | Execution Service |
| `siase-os-updates-queue` | Billing, Execution | OS Service |
| `siase-os-eventos` | OS Service | Auditoria/Monitoramento |

---

## Bancos de Dados

| Serviço | SQL (RDS) | NoSQL (DynamoDB) |
|---|---|---|
| OS Service | `siase_os_db` | `siase-os-eventos` |
| Billing Service | `siase_billing_db` | — |
| Execution Service | `siase_exec_db` | `siase-execution-fila` |

---

## Testes e Qualidade

| Tipo | Ferramenta | Cobertura Mínima |
|---|---|---|
| Unitários | JUnit 5 + Mockito | 80% por serviço |
| BDD | Cucumber 7 (todos os fluxos) | Todos os cenários |
| Qualidade | JaCoCo + SonarQube | 80% linhas |

### Fluxos BDD cobertos (por serviço)

**OS Service:**
- Criar OS com sucesso
- Avançar status
- Aprovar/recusar orçamento
- Cancelar OS
- Fluxo completo até entrega
- Rejeição de OS duplicada por veículo

**Billing Service:**
- Gerar orçamento a partir de evento
- Integração Mercado Pago (mock)
- Confirmar pagamento
- Compensação por OS cancelada

**Execution Service:**
- Enfileirar OS aprovada
- Iniciar execução
- Finalizar execução
- Remover da fila por cancelamento

---

## Documentação Arquitetural

- [ADR-001 — Arquitetura Hexagonal](docs/adr/ADR-001-arquitetura-hexagonal.md) *(Fase 1-3)*
- [ADR-002 — REST e JWT](docs/adr/ADR-002-rest-jwt.md) *(Fase 1-3)*
- [ADR-003 — Migração para Microsserviços](docs/adr/ADR-003-migracao-microservicos-fase4.md) *(Fase 4)*
- [ADR-004 — AWS SQS como Mensageria](docs/adr/ADR-004-aws-sqs-mensageria.md) *(Fase 4)*
- [RFC-001 — Fluxo de OS e Máquina de Estados](docs/rfc/RFC-001-fluxo-ordens-servico.md) *(Fase 1-3)*
- [RFC-002 — Integração Mercado Pago](docs/rfc/RFC-002-integracao-mercado-pago.md) *(Fase 4)*
- [Diagramas de Sequência v1](docs/diagramas-sequencia-v1.md) *(Fase 3)*
- [Diagramas de Sequência v2](docs/diagramas-sequencia-v2.md) *(Fase 4)*

---

## Execução Local (Fase 4)

Cada microsserviço tem seu próprio `docker-compose` ou pode ser executado individualmente. Consulte o README de cada repositório.

```bash
# OS Service
cd siase-os-service
mvn clean verify          # build + testes + BDD
mvn spring-boot:run       # execução local

# Billing Service
cd siase-billing-service
mvn clean verify
mvn spring-boot:run

# Execution Service
cd siase-execution-service
mvn clean verify
mvn spring-boot:run
```

---

## Links

- **OS Service Swagger:** `http://localhost:8081/api/swagger-ui.html`
- **Billing Service Swagger:** `http://localhost:8082/api/swagger-ui.html`
- **Execution Service Swagger:** `http://localhost:8083/api/swagger-ui.html`
- **Postman Collection:** `postman/SIASE.postman_collection.json`
- **Vídeo demonstrativo:** [A ser adicionado após gravação]
