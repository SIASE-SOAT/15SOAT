# Diagramas de Sequencia — siase-app (15SOAT)

## 1. Autenticacao Administrativa (Login do Mecanico)

```
Cliente HTTP     AuthController     AuthenticationManager    JwtService    DB (usuarios)
     │                 │                     │                    │               │
     │  POST /auth/login                     │                    │               │
     │  { username, password }               │                    │               │
     │────────────────►│                     │                    │               │
     │                 │                     │                    │               │
     │                 │ authenticate(       │                    │               │
     │                 │  username, password)│                    │               │
     │                 │────────────────────►│                    │               │
     │                 │                     │                    │               │
     │                 │                     │ loadUserByUsername │               │
     │                 │                     │────────────────────────────────── ►│
     │                 │                     │◄───────────────────────────────────│
     │                 │                     │                    │               │
     │                 │                     │ BCrypt.matches()   │               │
     │                 │                     │                    │               │
     │                 │                     │ [senha incorreta]  │               │
     │◄────────────────│ 401 Unauthorized    │                    │               │
     │                 │                     │                    │               │
     │                 │                     │ [senha correta]    │               │
     │                 │◄────────────────────│                    │               │
     │                 │                     │                    │               │
     │                 │ generateToken(      │                    │               │
     │                 │  userDetails)       │                    │               │
     │                 │─────────────────────────────────────────►│               │
     │                 │◄─────────────────────────────────────────│               │
     │                 │                     │                    │               │
     │  200 { token,   │                     │                    │               │
     │  expirationMs } │                     │                    │               │
     │◄────────────────│                     │                    │               │
```

## 2. Abertura de Ordem de Servico

```
Mecanico    OrdemServicoController    CriarOrdemServicoUC    Repositories    EmailPort
    │                │                        │                    │              │
    │  POST /api/ordens                       │                    │              │
    │  Authorization: Bearer <jwt>            │                    │              │
    │  { clienteId, veiculoId,                │                    │              │
    │    servicos, pecas }                    │                    │              │
    │───────────────►│                        │                    │              │
    │                │                        │                    │              │
    │                │ [JWT valido]           │                    │              │
    │                │ criarOrdemServicoUC    │                    │              │
    │                │ .executar(request)     │                    │              │
    │                │───────────────────────►│                    │              │
    │                │                        │                    │              │
    │                │                        │ busca cliente      │              │
    │                │                        │───────────────────►│              │
    │                │                        │◄───────────────────│              │
    │                │                        │                    │              │
    │                │                        │ busca veiculo      │              │
    │                │                        │───────────────────►│              │
    │                │                        │◄───────────────────│              │
    │                │                        │                    │              │
    │                │                        │ valida estoque     │              │
    │                │                        │ das pecas          │              │
    │                │                        │───────────────────►│              │
    │                │                        │◄───────────────────│              │
    │                │                        │                    │              │
    │                │                        │ calcula orcamento  │              │
    │                │                        │ (pecas + servicos) │              │
    │                │                        │                    │              │
    │                │                        │ cria OS (RECEBIDA) │              │
    │                │                        │ decrementa estoque │              │
    │                │                        │───────────────────►│              │
    │                │                        │◄───────────────────│              │
    │                │                        │                    │              │
    │                │                        │ registra metrica   │              │
    │                │                        │ ordemServicoCriada()              │
    │                │                        │                    │              │
    │                │◄───────────────────────│                    │              │
    │  201 Created   │                        │                    │              │
    │  Location: /api/ordens/{id}             │                    │              │
    │◄───────────────│                        │                    │              │
```

## 3. Fluxo de Aprovacao de Orcamento pelo Cliente

```
Cliente    OrdemServicoController    AprovarOrcamentoUC    OrdemServicoRepo    EmailPort
    │                │                       │                    │                │
    │  PATCH /api/ordens/acompanhar          │                    │                │
    │  /{numero}/aprovar-orcamento           │                    │                │
    │  (sem autenticacao)                    │                    │                │
    │───────────────►│                       │                    │                │
    │                │                       │                    │                │
    │                │ aprovarOrcamentoUC    │                    │                │
    │                │ .aprovar(numero)      │                    │                │
    │                │──────────────────────►│                    │                │
    │                │                       │                    │                │
    │                │                       │ busca OS por numero│                │
    │                │                       │───────────────────►│                │
    │                │                       │◄───────────────────│                │
    │                │                       │                    │                │
    │                │                       │ [status != AGUARDANDO_APROVACAO]    │
    │                │◄──────────────────────│ 422 BusinessException               │
    │  422           │                       │                    │                │
    │◄───────────────│                       │                    │                │
    │                │                       │                    │                │
    │                │                       │ [status correto]   │                │
    │                │                       │ transicao →        │                │
    │                │                       │ APROVADO           │                │
    │                │                       │ registra tempo     │                │
    │                │                       │ no status anterior │                │
    │                │                       │───────────────────►│                │
    │                │                       │◄───────────────────│                │
    │                │                       │                    │                │
    │                │◄──────────────────────│                    │                │
    │  200 OS atualizada                     │                    │                │
    │◄───────────────│                       │                    │                │
```

## 4. Avanco de Status pelo Mecanico

```
Mecanico    OrdemServicoController    AvancarStatusUC    OrdemServicoRepo    ObservabilityPort
    │                │                      │                  │                    │
    │  PATCH /api/ordens/{id}/avancar       │                  │                    │
    │  Authorization: Bearer <jwt>          │                  │                    │
    │───────────────►│                      │                  │                    │
    │                │                      │                  │                    │
    │                │ avancarStatusUC      │                  │                    │
    │                │ .executar(id)        │                  │                    │
    │                │─────────────────────►│                  │                    │
    │                │                      │                  │                    │
    │                │                      │ busca OS por id  │                    │
    │                │                      │─────────────────►│                    │
    │                │                      │◄─────────────────│                    │
    │                │                      │                  │                    │
    │                │                      │ calcula proximo  │                    │
    │                │                      │ status valido    │                    │
    │                │                      │                  │                    │
    │                │                      │ [OS ENTREGUE ou CANCELADA]            │
    │                │◄─────────────────────│ 422 BusinessException                 │
    │  422           │                      │                  │                    │
    │◄───────────────│                      │                  │                    │
    │                │                      │                  │                    │
    │                │                      │ [transicao valida]                    │
    │                │                      │ registra tempo   │                    │
    │                │                      │ no status atual  │                    │
    │                │                      │ tempoStatus()    │                    │
    │                │                      │──────────────────────────────────────►│
    │                │                      │                  │                    │
    │                │                      │ salva OS com     │                    │
    │                │                      │ novo status      │                    │
    │                │                      │─────────────────►│                    │
    │                │                      │◄─────────────────│                    │
    │                │◄─────────────────────│                  │                    │
    │  200 OS atualizada                    │                  │                    │
    │◄───────────────│                      │                  │                    │
```

## 5. Requisicao Autenticada via API Gateway (Fase 3)

```
Cliente    API Gateway    Lambda Authorizer    siase-app    Use Case    Repository
    │            │                │                │             │            │
    │  GET /api/ordens            │                │             │            │
    │  Authorization:             │                │             │            │
    │  Bearer <jwt-cliente>       │                │             │            │
    │───────────►│                │                │             │            │
    │            │                │                │             │            │
    │            │ invoca         │                │             │            │
    │            │ Authorizer     │                │             │            │
    │            │───────────────►│                │             │            │
    │            │                │                │             │            │
    │            │                │ verifica JWT   │             │            │
    │            │                │ (assinatura,   │             │            │
    │            │                │  issuer, exp,  │             │            │
    │            │                │  clienteId)    │             │            │
    │            │                │                │             │            │
    │            │ { isAuthorized:│                │             │            │
    │            │   true,        │                │             │            │
    │            │   context }    │                │             │            │
    │            │◄───────────────│                │             │            │
    │            │                │                │             │            │
    │            │ encaminha para │                │             │            │
    │            │ siase-app      │                │             │            │
    │            │ + contexto JWT │                │             │            │
    │            │────────────────────────────────►│             │            │
    │            │                │                │             │            │
    │            │                │                │ executa     │            │
    │            │                │                │ use case    │            │
    │            │                │                │────────────►│            │
    │            │                │                │             │───────────►│
    │            │                │                │             │◄───────────│
    │            │                │                │◄────────────│            │
    │            │◄────────────────────────────────│             │            │
    │  200 resposta               │                │             │            │
    │◄───────────│                │                │             │            │
```
