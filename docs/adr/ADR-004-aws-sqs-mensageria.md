# ADR-004 — AWS SQS como Broker de Mensageria

**Status:** Aceito  
**Data:** 2025-01  
**Contexto:** Fase 4 — Escolha de mensageria para Saga Coreografado

## Contexto

O Saga Coreografado exige um broker de mensageria confiável. As opções avaliadas:

| Opção | Prós | Contras |
|---|---|---|
| RabbitMQ | Simples, roteamento flexível | Cluster a gerenciar, não nativo AWS |
| Apache Kafka | Alta throughput, replay | Complexidade operacional alta |
| **AWS SQS** | Serverless, nativo AWS, gerenciado | Sem replay nativo (usar SNS+SQS para fan-out) |
| AWS SNS+SQS | Fan-out nativo | Mais componentes |

## Decisão

Usar **AWS SQS** com filas dedicadas por fluxo de comunicação:

```
siase-billing-queue     → OS Service → Billing Service
siase-execution-queue   → OS Service + Billing → Execution Service
siase-os-updates-queue  → Billing + Execution → OS Service
siase-os-eventos        → OS Service → Auditoria/Monitoramento
```

### Configuração das filas

- **Tipo**: Standard Queue (alta throughput, at-least-once delivery)
- **Visibility Timeout**: 30s (tempo para processar antes de reaparecer)
- **Message Retention**: 4 dias
- **Dead Letter Queue**: configurada para cada fila (após 3 tentativas)
- **Long Polling**: 5-20s (reduz custo e latência)

## Idempotência

Como SQS garante at-least-once (não exactly-once), todos os consumidores são idempotentes usando `osNumero` como chave de deduplicação.

## Consequências

- **Positivo**: zero operação de cluster, integração nativa com IAM/CloudWatch
- **Positivo**: DLQ automática para mensagens com falha
- **Negativo**: sem replay de mensagens antigas (mitigado pelo DynamoDB de eventos)
