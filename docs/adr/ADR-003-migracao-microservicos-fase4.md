# ADR-003 — Migração para Arquitetura de Microsserviços (Fase 4)

**Status:** Aceito  
**Data:** 2025-01  
**Contexto:** Fase 4 — Escala nacional com múltiplas filiais

## Contexto

O monólito hexagonal da Fase 3 (siase-app) atingiu seus limites de escala. Com a expansão nacional da oficina, surgiram requisitos de:

- Deploy independente por domínio de negócio
- Escalabilidade seletiva (ex: billing escala mais que OS em períodos de cobrança)
- Resiliência isolada (falha no billing não afeta abertura de OS)
- Times independentes por domínio

## Decisão

Refatorar para **3 microsserviços independentes**, cada um com:
- Repositório Git próprio
- Banco de dados próprio (sem compartilhamento)
- Pipeline CI/CD independente
- Deploy Kubernetes independente

| Microsserviço | Porta | Banco SQL | Banco NoSQL |
|---|---|---|---|
| `siase-os-service` | 8081 | RDS PostgreSQL `siase_os_db` | DynamoDB `siase-os-eventos` |
| `siase-billing-service` | 8082 | RDS PostgreSQL `siase_billing_db` | — |
| `siase-execution-service` | 8083 | RDS PostgreSQL `siase_exec_db` | DynamoDB `siase-execution-fila` |

## Alternativas Consideradas

| Alternativa | Motivo da Rejeição |
|---|---|
| Manter monólito modular | Não atende requisito de deploy independente |
| 2 microsserviços | Billing + Execution muito acoplados sem separação |
| Event Sourcing completo | Complexidade excessiva para o prazo |

## Consequências

- **Positivo**: escala, resiliência e deploy independentes por domínio
- **Negativo**: consistência eventual, overhead operacional de 3 serviços
- **Mitigação**: Saga Coreografado com compensação, DynamoDB para auditoria
