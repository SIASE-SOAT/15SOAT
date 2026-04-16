# Guia de Testes — SIASE via Postman

> **Pré-requisitos**
> - Aplicação rodando em `http://localhost:8080`
> - Coleção `SIASE.postman_collection.json` importada no Postman
> - PostgreSQL ativo e migrations aplicadas (Flyway roda automaticamente ao iniciar)

---

## Como as variáveis funcionam

A coleção usa variáveis de coleção (`{{variavel}}`) que são **preenchidas automaticamente** pelos scripts de teste de cada requisição. Você não precisa copiar e colar IDs manualmente — basta executar as requisições na ordem indicada.

| Variável | Preenchida por |
|---|---|
| `{{jwtToken}}` | Login |
| `{{clienteId}}` | Criar cliente |
| `{{veiculoId}}` | Criar veículo |
| `{{pecaId}}` | Criar peça |
| `{{servicoId}}` | Criar serviço |
| `{{osId}}` | Criar OS |
| `{{osNumero}}` | Criar OS |
| `{{pagamentoId}}` | Registrar pagamento |
| `{{pedidoCompraId}}` | Criar pedido de compra |
| `{{agendamentoId}}` | Criar agendamento |

---

## Cenário 1 — Ciclo de vida completo de uma OS (fluxo feliz)

Este cenário cobre o fluxo principal: da criação do cadastro até a entrega do veículo com pagamento confirmado.

### Passo 1 — Criar usuário do sistema

**Requisição:** `Autenticação > Registrar usuário`

```json
{
  "username": "atendente1",
  "password": "Atend@2024"
}
```

**Resposta esperada:** `201 Created`

> Só é necessário registrar uma vez. Se o usuário já existir, pule para o Passo 2.

---

### Passo 2 — Fazer login e obter token JWT

**Requisição:** `Autenticação > Login`

```json
{
  "username": "atendente1",
  "password": "Atend@2024"
}
```

**Resposta esperada:** `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

> O token é salvo automaticamente em `{{jwtToken}}` e enviado no header `Authorization: Bearer` em todas as requisições subsequentes.

---

### Passo 3 — Cadastrar uma peça no estoque

**Requisição:** `Peças e Insumos > Criar peça`

```json
{
  "codigo": "FILTRO-OLEO-001",
  "nome": "Filtro de Óleo Motor",
  "descricao": "Filtro de óleo para motores 1.0 a 2.0",
  "preco": 45.90,
  "quantidadeEstoque": 50,
  "estoqueMinimo": 10,
  "unidadeMedida": "UN"
}
```

**Resposta esperada:** `201 Created` — `{{pecaId}}` é salvo automaticamente.

> O campo `estoqueAbaixoMinimo` na resposta indica se o estoque está abaixo do mínimo configurado.

---

### Passo 4 — Cadastrar um serviço

**Requisição:** `Serviços > Criar serviço`

```json
{
  "nome": "Troca de Óleo e Filtro",
  "descricao": "Troca completa do óleo motor com substituição do filtro",
  "preco": 120.00
}
```

**Resposta esperada:** `201 Created` — `{{servicoId}}` é salvo automaticamente.

---

### Passo 5 — Cadastrar um cliente

**Requisição:** `Clientes > Criar cliente (PF)`

```json
{
  "nome": "João da Silva",
  "tipoPessoa": "PF",
  "documento": "529.982.247-25",
  "email": "joao.silva@email.com",
  "telefone": "(11) 99999-1234",
  "endereco": "Rua das Flores, 123 - São Paulo/SP"
}
```

**Resposta esperada:** `201 Created` — `{{clienteId}}` é salvo automaticamente.

---

### Passo 6 — Cadastrar um veículo para o cliente

**Requisição:** `Veículos > Criar veículo`

```json
{
  "placa": "ABC1234",
  "marca": "Toyota",
  "modelo": "Corolla",
  "ano": 2022,
  "cor": "Prata",
  "clienteId": "{{clienteId}}"
}
```

**Resposta esperada:** `201 Created` — `{{veiculoId}}` é salvo automaticamente.

---

### Passo 7 — Abrir uma Ordem de Serviço

**Requisição:** `Ordens de Serviço > Criar OS (com serviço e peça)`

```json
{
  "clienteId": "{{clienteId}}",
  "veiculoId": "{{veiculoId}}",
  "observacoes": "Cliente relata barulho ao frear e óleo baixo no painel",
  "itensServico": [
    {
      "servicoId": "{{servicoId}}",
      "observacoes": "Usar óleo sintético 5W30"
    }
  ],
  "itensPeca": [
    {
      "pecaId": "{{pecaId}}",
      "quantidade": 2
    }
  ]
}
```

**Resposta esperada:** `201 Created` — `{{osId}}` e `{{osNumero}}` são salvos automaticamente.

> O estoque da peça é **deduzido automaticamente** neste momento (50 → 48 unidades).
> O preço dos serviços e peças é um **snapshot** do catálogo no momento da criação.

---

### Passo 8 — Avançar status: RECEBIDA → EM_DIAGNOSTICO

**Requisição:** `Ordens de Serviço > Avancar status → EM_DIAGNOSTICO`

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta esperada:** `200 OK` com `"status": "EM_DIAGNOSTICO"`

---

### Passo 9 — Avançar status: EM_DIAGNOSTICO → AGUARDANDO_APROVACAO

**Requisição:** `Ordens de Serviço > Avancar status → AGUARDANDO_APROVACAO`

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta esperada:** `200 OK` com `"status": "AGUARDANDO_APROVACAO"`

> Um e-mail de orçamento é disparado para o cliente (visível nos logs do servidor com o prefixo `[EMAIL]`).

---

### Passo 10 — Avançar status: AGUARDANDO_APROVACAO → EM_EXECUCAO (cliente aprova)

**Requisição:** `Ordens de Serviço > Avancar status → EM_EXECUCAO (aprovação)`

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta esperada:** `200 OK` com `"status": "EM_EXECUCAO"`

> Um e-mail de confirmação de aprovação é disparado (visível nos logs).

---

### Passo 11 — Avançar status: EM_EXECUCAO → FINALIZADA

**Requisição:** `Ordens de Serviço > Avancar status → FINALIZADA`

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta esperada:** `200 OK` com `"status": "FINALIZADA"`

---

### Passo 12 — Registrar pagamento

**Requisição:** `Pagamentos > Registrar pagamento da OS`

```json
{
  "formaPagamento": "PIX",
  "valor": 211.80
}
```

`POST /api/ordens/{{osId}}/pagamento`

**Resposta esperada:** `201 Created` — `{{pagamentoId}}` é salvo automaticamente.

> Formas aceitas: `DINHEIRO` | `CARTAO_DEBITO` | `CARTAO_CREDITO` | `PIX` | `TRANSFERENCIA`

---

### Passo 13 — Confirmar pagamento (OS avança para ENTREGUE)

**Requisição:** `Pagamentos > Confirmar pagamento`

`PATCH /api/pagamentos/{{pagamentoId}}/confirmar`

**Resposta esperada:** `200 OK` com `"status": "PAGO"`

> Ao confirmar o pagamento, a OS avança **automaticamente** de `FINALIZADA` para `ENTREGUE` e o e-mail de confirmação é disparado (visível nos logs).

---

### Passo 14 — Verificar OS entregue

**Requisição:** `Ordens de Serviço > Buscar OS por ID`

`GET /api/ordens/{{osId}}`

**Resultado esperado:**

```json
{
  "status": "ENTREGUE",
  "totalServicos": 120.00,
  "totalPecas": 91.80,
  "total": 211.80,
  "dataFechamento": "2026-..."
}
```

---

### Passo 15 — Acompanhar OS pelo número (endpoint público, sem token)

**Requisição:** `Ordens de Serviço > Acompanhar OS pelo número (público, sem token)`

`GET /api/ordens/acompanhar/{{osNumero}}`

> Este endpoint **não exige autenticação**. Simula o portal do cliente que consulta o andamento da OS pelo número (ex: `OS-20260414-A3F7C2`).

---

## Cenário 2 — Adicionar peças a uma OS durante a execução

Este cenário testa a adição de peças a uma OS já criada, cenário comum quando diagnósticos adicionais revelam a necessidade de mais insumos.

> Repita os Passos 1 a 11 do Cenário 1 (ou crie uma nova OS até o status `EM_EXECUCAO`).

### Passo 1 — Verificar OS atual

**Requisição:** `Ordens de Serviço > Buscar OS por ID`

`GET /api/ordens/{{osId}}`

Anote o `total` atual e a quantidade de peças em `itensPeca`.

### Passo 2 — Criar uma nova peça (se necessário)

Se quiser testar com uma peça diferente, execute `Peças e Insumos > Criar peça` e salve o novo `{{pecaId}}`.

Caso contrário, use um `{{pecaId}}` já existente com estoque disponível.

### Passo 3 — Adicionar peça à OS

**Requisição:** `Ordens de Serviço > Adicionar peça a OS existente`

```json
{
  "pecaId": "{{pecaId}}",
  "quantidade": 1
}
```

`POST /api/ordens/{{osId}}/items-peca`

**Resposta esperada:** `200 OK` com a OS atualizada.

**Validações na resposta:**
- `itensPeca` contém a nova peça
- `totalPecas` foi recalculado e aumentou
- `total` foi recalculado (totalServicos + totalPecas)
- Novo `totalPecas = totalPecas_anterior + (precoUnitario × quantidade)`

### Passo 4 — Verificar que o estoque foi deduzido

`GET /api/pecas/{{pecaId}}`

O campo `quantidadeEstoque` deve ter diminuído de acordo com a quantidade adicionada.

### Passo 5 — Verificar impossibilidade de adicionar duplicada

Tente adicionar a **mesma peça** novamente:

`POST /api/ordens/{{osId}}/items-peca`

```json
{
  "pecaId": "{{pecaId}}",
  "quantidade": 1
}
```

**Resposta esperada:** `422 Unprocessable Entity`

```json
{
  "message": "Esta peça já foi adicionada a esta OS."
}
```

### Passo 6 — Testar validações

| Cenário | Body | Resposta esperada |
|---|---|---|
| Peça não existe | `{"pecaId": "id-invalido", "quantidade": 1}` | `404 Not Found` — "Peça não encontrada" |
| Peça inativa | `{"pecaId": "id-inativo", "quantidade": 1}` | `422` — "Peça não está ativa" |
| Estoque insuficiente | `{"pecaId": "{{pecaId}}", "quantidade": 999}` | `422` — "Estoque insuficiente para a peça" |
| Status não permitido | Criar OS → avançar para `FINALIZADA` → adicionar peça | `422` — "Status não permite adição de peças" |

---

## Cenário 3 — Cancelamento de OS

Este cenário testa o cancelamento e a **devolução automática do estoque**.

> Repita os Passos 1 a 7 do Cenário 1 (ou use variáveis já salvas se acabou de executar o fluxo acima).

### Passo 1 — Abrir nova OS

Execute `Ordens de Serviço > Criar OS (com serviço e peça)` novamente.

Verifique o estoque da peça **antes**: `GET /api/pecas/{{pecaId}}`

Anote o campo `quantidadeEstoque`.

### Passo 2 — Avançar para EM_DIAGNOSTICO (opcional)

Execute `Avancar status → EM_DIAGNOSTICO`.

> O cancelamento é permitido nos status `RECEBIDA`, `EM_DIAGNOSTICO` e `AGUARDANDO_APROVACAO`.

### Passo 3 — Cancelar a OS

**Requisição:** `Ordens de Serviço > Cancelar OS`

`PATCH /api/ordens/{{osId}}/cancelar`

**Resposta esperada:** `200 OK` com `"status": "CANCELADA"` e `dataFechamento` preenchida.

> Um e-mail de cancelamento é disparado (visível nos logs).

### Passo 4 — Verificar devolução do estoque

`GET /api/pecas/{{pecaId}}`

O campo `quantidadeEstoque` deve ter **voltado ao valor anterior** à criação da OS.

### Casos de erro a validar

| Situação | Endpoint | HTTP esperado | Mensagem |
|---|---|---|---|
| Cancelar OS em `EM_EXECUCAO` | `PATCH /ordens/{id}/cancelar` | `422` | "Ordem de serviço em execução ou finalizada não pode ser cancelada." |
| Cancelar OS `ENTREGUE` | `PATCH /ordens/{id}/cancelar` | `422` | "Ordem de serviço já entregue não pode ser cancelada." |
| Avançar OS `CANCELADA` | `PATCH /ordens/{id}/avancar` | `422` | "Ordem de serviço cancelada não pode ser avançada." |
| Criar OS com estoque insuficiente | `POST /ordens` | `422` | "Estoque insuficiente para a peça: ..." |

---

## Cenário 4 — Alerta de estoque crítico

Este cenário demonstra o alerta de estoque abaixo do mínimo.

### Passo 1 — Criar peça com estoque próximo do mínimo

`Peças e Insumos > Criar peça`

```json
{
  "codigo": "CORREIA-DIST-002",
  "nome": "Correia Dentada",
  "descricao": "Correia dentada para motores 1.6 e 2.0",
  "preco": 189.90,
  "quantidadeEstoque": 3,
  "estoqueMinimo": 5,
  "unidadeMedida": "UN"
}
```

> `quantidadeEstoque` (3) já está abaixo de `estoqueMinimo` (5). A resposta retorna `"estoqueAbaixoMinimo": true`.

### Passo 2 — Criar OS usando essa peça

Repita os Passos 5–7 do Cenário 1 (cliente + veículo + OS), usando a nova `{{pecaId}}` com quantidade `1`.

Após criar a OS, consulte a peça novamente — o estoque cai para `2` e `estoqueAbaixoMinimo` permanece `true`.

### Passo 3 — Repor estoque via entrada manual

`Peças e Insumos > Entrada de estoque`

```json
{
  "operacao": "ENTRADA",
  "quantidade": 10
}
```

O estoque sobe para `12` e `estoqueAbaixoMinimo` passa a `false`.

---

## Cenário 4 — Reposição de estoque via Pedido de Compra

Fluxo alternativo ao Cenário 3 para casos onde a reposição precisa de aprovação.

### Passo 1 — Criar pedido de compra

`Pedidos de Compra > Criar pedido de compra`

```json
{
  "pecaId": "{{pecaId}}",
  "quantidadeSolicitada": 20,
  "observacoes": "Estoque crítico — repor urgente"
}
```

`{{pedidoCompraId}}` é salvo automaticamente.

### Passo 2 — Aprovar pedido

`Pedidos de Compra > Aprovar pedido de compra`

`PATCH /api/pedidos-compra/{{pedidoCompraId}}/aprovar`

Status: `PENDENTE → APROVADO`

### Passo 3 — Receber mercadoria

`Pedidos de Compra > Receber pedido de compra`

`PATCH /api/pedidos-compra/{{pedidoCompraId}}/receber?quantidade=20`

Status: `APROVADO → RECEBIDO`

> O estoque da peça é **incrementado automaticamente** pela quantidade recebida.

---

## Cenário 5 — Agendamento vinculado a uma OS

### Passo 1 — Criar agendamento (requer clienteId e veiculoId)

`Agendamentos > Criar agendamento`

```json
{
  "clienteId": "{{clienteId}}",
  "veiculoId": "{{veiculoId}}",
  "dataHora": "2026-05-20T10:00:00",
  "descricaoServicos": "Revisão de 20.000 km e troca de óleo"
}
```

> Altere a data para um momento futuro antes de enviar.

### Passo 2 — Confirmar agendamento

`Agendamentos > Confirmar agendamento`

`PATCH /api/agendamentos/{{agendamentoId}}/confirmar`

Status: `AGENDADO → CONFIRMADO`

### Passo 3 — Cliente chegou: abrir OS normalmente

Com o agendamento confirmado, execute o Cenário 1 a partir do Passo 7 (Criar OS).

---

## Referência rápida — Status da OS

```
RECEBIDA
   ↓ avancar
EM_DIAGNOSTICO
   ↓ avancar               ← cancelar disponível até aqui
AGUARDANDO_APROVACAO
   ↓ avancar (cliente aprova)
EM_EXECUCAO               ← cancelar NÃO permitido a partir daqui
   ↓ avancar
FINALIZADA
   ↓ (automático ao confirmar pagamento)
ENTREGUE
```

## Referência rápida — Códigos HTTP da API

| Código | Significado |
|---|---|
| `200` | OK — operação realizada com sucesso |
| `201` | Created — recurso criado com sucesso |
| `204` | No Content — remoção realizada (soft delete) |
| `400` | Bad Request — dados inválidos (validação de campos) |
| `401` | Unauthorized — token ausente ou expirado |
| `403` | Forbidden — token válido, mas sem permissão |
| `404` | Not Found — recurso não encontrado |
| `409` | Conflict — violação de unicidade (ex: código de peça duplicado) |
| `422` | Unprocessable Entity — regra de negócio violada (ex: estoque insuficiente, status inválido para a operação) |
