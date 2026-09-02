# RFC-001 — Fluxo de Ordens de Servico e Maquina de Estados

**Status:** Implementado
**Data:** 2026
**Repositorio:** 15SOAT (siase-app)

## Resumo

Este documento descreve o ciclo de vida completo de uma Ordem de Servico (OS) no SIASE, a maquina de estados dos status, as transicoes validas e as regras de negocio associadas a cada transicao.

## Maquina de Estados

```
                    ┌─────────────┐
                    │   RECEBIDA  │
                    └──────┬──────┘
                           │ avancar()
                           ▼
                    ┌─────────────────┐
                    │ EM_DIAGNOSTICO  │
                    └──────┬──────────┘
                           │ avancar()
                           ▼
                    ┌──────────────────────┐
                    │ AGUARDANDO_APROVACAO │
                    └──────┬───────────────┘
                           │
              ┌────────────┴────────────┐
              │ aprovar()               │ recusar()
              ▼                         ▼
        ┌──────────┐             ┌───────────┐
        │ APROVADO │             │ CANCELADA │
        └────┬─────┘             └───────────┘
             │ avancar()
             ▼
        ┌─────────────┐
        │ EM_EXECUCAO │
        └──────┬──────┘
               │ avancar()
               ▼
        ┌───────────┐
        │ FINALIZADA│
        └─────┬─────┘
              │ avancar()
              ▼
        ┌──────────┐
        │ ENTREGUE │
        └──────────┘

  cancelar() e valido em: RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO
```

## Regras de Negocio por Transicao

### RECEBIDA → EM_DIAGNOSTICO
- Acionada pelo mecanico ao iniciar o diagnostico do veiculo.
- Nenhuma validacao adicional alem do status atual.

### EM_DIAGNOSTICO → AGUARDANDO_APROVACAO
- Acionada apos o diagnostico concluido.
- O orcamento (valor total de pecas + servicos) e calculado automaticamente.
- Um email e enviado ao cliente com o link do portal de acompanhamento.

### AGUARDANDO_APROVACAO → APROVADO
- Acionada pelo cliente via portal publico (`PATCH /acompanhar/{numero}/aprovar-orcamento`).
- Nao exige autenticacao JWT — o numero da OS e o identificador publico.

### AGUARDANDO_APROVACAO → CANCELADA
- Acionada pelo cliente via portal publico (`PATCH /acompanhar/{numero}/recusar-orcamento`).
- O estoque das pecas reservadas e devolvido.

### APROVADO → EM_EXECUCAO
- Acionada pelo mecanico ao iniciar a execucao dos servicos.

### EM_EXECUCAO → FINALIZADA
- Acionada pelo mecanico apos conclusao de todos os servicos.
- Os timestamps de `iniciado_em` e `finalizado_em` dos itens de servico alimentam a metrica de tempo medio.

### FINALIZADA → ENTREGUE
- Acionada pelo mecanico na entrega do veiculo ao cliente.

### cancelar()
- Valido nos status: RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO.
- Devolve o estoque das pecas adicionadas a OS.

## Listagem de OS

A listagem administrativa (`GET /api/ordens`) exclui OS com status FINALIZADA e ENTREGUE e ordena por:

1. Prioridade de status: EM_EXECUCAO > AGUARDANDO_APROVACAO > APROVADO > EM_DIAGNOSTICO > RECEBIDA
2. Data de criacao: mais antigas primeiro (dentro do mesmo status)

Essa ordenacao garante que as OS mais urgentes aparecem no topo da fila do mecanico.

## Acompanhamento Publico

O endpoint `GET /api/ordens/acompanhar/{numero}` e publico e retorna o status atual da OS, o veiculo, os servicos e o valor total. O `numero` e um identificador legivel (ex: `OS-2024-00001`) diferente do UUID interno, evitando enumeracao de recursos.

## Observabilidade

Cada transicao de status registra:
- `siase.ordens.servico.criadas` (Counter): incrementado na criacao da OS.
- `siase.ordem.servico.tempo.status` (Timer): tempo que a OS permaneceu em cada status.
- `siase.execucao.item.iniciadas` (Counter): incrementado ao iniciar execucao de um item.
- `siase.execucao.item.tempo` (Timer): tempo de execucao de cada item de servico.
- `siase.falhas.integracao` (Counter): incrementado em falhas de integracao externa (email, webhook).
