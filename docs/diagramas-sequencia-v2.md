# Diagramas de Sequência — v2 (Fase 4 — Microsserviços)

> **v1 (Fase 3 — Monólito Hexagonal):** [diagramas-sequencia-v1.md](diagramas-sequencia-v1.md)

---

## Visão Geral da Arquitetura de Microsserviços

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
                   ┌──────────────────────────────────────┐
                   │              EKS (AWS)               │
                   │                                      │
                   │  ┌─────────────────────────────────┐ │
                   │  │      siase-os-service           │ │
                   │  │  (OS, status, aprovação)        │ │
                   │  └──────────────┬──────────────────┘ │
                   │                 │ SQS                 │
                   │  ┌──────────────▼──────────────────┐ │
                   │  │    siase-billing-service        │ │
                   │  │  (orçamentos, Mercado Pago)     │ │
                   │  └──────────────┬──────────────────┘ │
                   │                 │ SQS                 │
                   │  ┌──────────────▼──────────────────┐ │
                   │  │   siase-execution-service       │ │
                   │  │  (fila, execução de serviços)   │ │
                   │  └─────────────────────────────────┘ │
                   └──────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────────┐
              │               │                   │
              ▼               ▼                   ▼
    RDS PostgreSQL      DynamoDB            AWS SQS
    (por serviço)    (histórico eventos)  (mensageria)
```

---

## 1. Fluxo Completo de OS — Saga Coreografado

```
Mecânico/Cliente   OS Service    SQS Billing   Billing Svc   SQS Execution  Execution Svc  SQS OS-Updates
       │               │              │              │               │              │              │
       │─POST /ordens─►│              │              │               │              │              │
       │               │─save(OS)─────│              │               │              │              │
       │               │─DynamoDB─────│              │               │              │              │
       │               │─OS_CRIADA───►│              │               │              │              │
       │◄─201 Created──│              │─consume─────►│               │              │              │
       │               │              │              │─gera orçamento│              │              │
       │               │              │              │─ORCAMENTO_GERADO──────────────────────────►│
       │               │              │              │               │              │              │─consume
       │               │◄─────────────────────────────────────────────────────────────────────────│
       │               │─update(AGUARDANDO_APROVACAO)│               │              │              │
       │               │              │              │               │              │              │
       │─PATCH /aprovar►│              │              │               │              │              │
       │               │─update(APROVADO)             │               │              │              │
       │               │─ORCAMENTO_APROVADO──────────────────────────►│              │              │
       │◄─200 OK───────│              │              │               │─consume─────►│              │
       │               │              │              │               │              │─inicia exec  │
       │               │              │              │               │              │─EXEC_INICIADA►│
       │               │              │              │               │              │              │─consume
       │               │◄─────────────────────────────────────────────────────────────────────────│
       │               │─update(EM_EXECUCAO)          │               │              │              │
       │               │              │              │               │              │─finaliza─────│
       │               │              │              │               │              │─EXEC_FINALIZADA►│
       │               │              │              │               │              │              │─consume
       │               │◄─────────────────────────────────────────────────────────────────────────│
       │               │─update(FINALIZADA)           │              │               │              │
       │               │              │─EXEC_FINALIZADA─────────────►│               │              │
       │               │              │              │─Mercado Pago  │               │              │
       │◄─notificação──────────────────────────────── │               │              │              │
       │─confirma pagto│              │              │─PAGAMENTO_CONFIRMADO──────────────────────►│
       │               │              │              │               │              │              │─consume
       │               │◄─────────────────────────────────────────────────────────────────────────│
       │               │─update(ENTREGUE)             │               │              │              │
       │◄─notificação──│              │              │               │              │              │
```

---

## 2. Fluxo de Compensação — Orçamento Recusado

```
Cliente         OS Service       SQS Billing    Billing Svc    SQS Execution  Execution Svc
    │               │                 │               │               │              │
    │─PATCH /recusar►│                 │               │               │              │
    │               │─update(CANCELADA)│               │               │              │
    │               │─OS_CANCELADA───►│               │               │              │
    │               │─OS_CANCELADA────────────────────────────────────►│              │
    │◄─200 OK───────│                 │─consume───────►│               │              │
    │               │                 │               │─cancela orçamento             │
    │               │                 │               │               │─consume──────►│
    │               │                 │               │               │              │─remove da fila
```

---

## 3. Autenticação de Cliente via API Gateway + Lambda (mantida da Fase 3)

```
Cliente    API Gateway    Lambda Token    RDS (clientes)    siase-os-service
    │            │               │               │                  │
    │─POST /auth/token           │               │                  │
    │  { cpf }   │               │               │                  │
    │───────────►│               │               │                  │
    │            │─invoca────────►│               │                  │
    │            │               │─busca cliente─►│                  │
    │            │               │◄──────────────│                  │
    │            │               │─valida CPF    │                  │
    │            │               │─gera JWT      │                  │
    │            │◄──────────────│               │                  │
    │◄───────────│               │               │                  │
    │  { token } │               │               │                  │
    │            │               │               │                  │
    │─GET /api/ordens/cliente/{id}               │                  │
    │  Authorization: Bearer <jwt>               │                  │
    │───────────►│               │               │                  │
    │            │─Lambda Authorizer verifica JWT│                  │
    │            │─encaminha para OS Service─────────────────────────►│
    │            │               │               │                  │─consulta OS
    │◄───────────────────────────────────────────────────────────────│
```

---

## 4. Criação de OS — Detalhe Interno do OS Service

```
Mecânico    Controller    CriarOsUC    Repository    SQS Publisher    DynamoDB
    │            │             │            │               │              │
    │─POST /ordens►│             │            │               │              │
    │            │─executar(req)►│            │               │              │
    │            │             │─existsByVeiculo►│             │              │
    │            │             │◄────────────│               │              │
    │            │             │─save(OS)────►│               │              │
    │            │             │◄────────────│               │              │
    │            │             │─salvar(evento)──────────────────────────────►│
    │            │             │─publicarOsCriada►│            │              │
    │            │◄────────────│            │               │              │
    │◄───────────│             │            │               │              │
    │  201 Created│             │            │               │              │
```

---

## 5. Consulta de Histórico de Eventos (DynamoDB)

```
Mecânico    OS Service    DynamoDB (siase-os-eventos)
    │            │                    │
    │─GET /ordens/{id}/eventos        │
    │───────────►│                    │
    │            │─query(osId-index)──►│
    │            │◄───────────────────│
    │◄───────────│                    │
    │  [ eventos ]│                    │
```
