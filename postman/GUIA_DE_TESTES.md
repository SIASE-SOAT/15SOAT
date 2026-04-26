# Guia de Testes — SIASE via Postman/Insomnia

> **Pré-requisitos**
> - Aplicação rodando em `http://localhost:8080`
> - Coleção `SIASE.postman_collection.json` importada
> - PostgreSQL ativo e migrations aplicadas (Flyway roda automaticamente ao iniciar)
> - Para resetar os dados entre testes: execute `postman/reset_base.sql` e limpe a variável `{{jwtToken}}`

---

## Como as variáveis funcionam

A coleção usa variáveis que são **preenchidas automaticamente** pelos scripts de teste. Você não precisa copiar e colar IDs manualmente.

| Variável | Preenchida por |
|---|---|
| `{{jwtToken}}` | Login |
| `{{clienteId}}` | Criar cliente |
| `{{veiculoId}}` | Criar veículo |
| `{{pecaId}}` | Criar peça |
| `{{servicoId}}` | Criar serviço |
| `{{itemServicoId}}` | Criar OS / Adicionar serviço |
| `{{osId}}` | Criar OS |
| `{{osNumero}}` | Criar OS |
| `{{pagamentoId}}` | Registrar pagamento |
| `{{pedidoCompraId}}` | Criar pedido de compra |

---

## Cenário 1 — Ciclo de vida completo de uma OS (fluxo feliz)

Fluxo principal do sistema: do cadastro até a entrega do veículo com pagamento confirmado.

### Passo 1 — Criar usuário do sistema

**Requisição:** `Autenticação > Registrar usuário`

```json
{
  "username": "atendente1",
  "password": "Atend@2024"
}
```

**Resposta esperada:** `201 Created`

> Só é necessário registrar uma vez. Se o usuário já existir, vá ao Passo 2.

---

### Passo 2 — Login e obtenção do token JWT

**Requisição:** `Autenticação > Login`

```json
{
  "username": "atendente1",
  "password": "Atend@2024"
}
```

**Resposta esperada:** `200 OK`

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```
> O token é salvo automaticamente em `{{jwtToken}}` e enviado em todas as requisições seguintes.

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

**Resposta esperada:** `201 Created` — `{{pecaId}}` salvo automaticamente.

> `estoqueAbaixoMinimo: false` quando `quantidadeEstoque (50) >= estoqueMinimo (10)`.

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

**Resposta esperada:** `201 Created` — `{{servicoId}}` salvo automaticamente.

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

**Resposta esperada:** `201 Created` — `{{clienteId}}` salvo automaticamente.

---

### Passo 6 — Cadastrar veículo para o cliente

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

**Resposta esperada:** `201 Created` — `{{veiculoId}}` salvo automaticamente.

---

### Passo 7 — Preparar abertura da OS por CPF/CNPJ e placa

**Requisição:** `Ordens de Serviço > Preparar abertura da OS (documento + placa)`

`GET /api/ordens/preparar-abertura?documento=52998224725&placa=ABC1234`

**Resposta esperada:** `200 OK`

```json
{
  "cliente": {
    "id": "{{clienteId}}",
    "nome": "João da Silva",
    "documento": "52998224725"
  },
  "veiculos": [
    {
      "id": "{{veiculoId}}",
      "placa": "ABC1234",
      "marca": "Toyota",
      "modelo": "Corolla",
      "ano": 2022,
      "ativo": true
    }
  ],
  "veiculoSelecionado": {
    "id": "{{veiculoId}}",
    "placa": "ABC1234",
    "marca": "Toyota",
    "modelo": "Corolla",
    "ano": 2022,
    "ativo": true
  },
  "prontoParaAbertura": true
}
```

> Esta etapa explicita no backend a identificação do cliente por CPF/CNPJ antes da abertura da OS.  
> Também valida que a placa informada pertence ao cliente identificado.

---

### Passo 8 — Abrir uma Ordem de Serviço

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

**Resposta esperada:** `201 Created` — `{{osId}}` e `{{osNumero}}` salvos automaticamente.

> O estoque da peça é **deduzido automaticamente** (50 → 48 unidades).  
> Os preços são **snapshot** do catálogo no momento da criação.

---

### Passo 9 — Avançar status: RECEBIDA → EM_DIAGNOSTICO

`Ordens de Serviço > Avancar status → EM_DIAGNOSTICO`

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta esperada:** `200 OK` com `"status": "EM_DIAGNOSTICO"`

---

### Passo 10 — Avançar status: EM_DIAGNOSTICO → AGUARDANDO_APROVACAO

`Ordens de Serviço > Avancar status → AGUARDANDO_APROVACAO`

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta esperada:** `200 OK` com `"status": "AGUARDANDO_APROVACAO"`

> E-mail de orçamento disparado para o cliente (visível nos logs com prefixo `[EMAIL]`).

---

### Passo 11 — Avançar status: AGUARDANDO_APROVACAO → EM_EXECUCAO

`Ordens de Serviço > Avancar status → EM_EXECUCAO (aprovação)`

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta esperada:** `200 OK` com `"status": "EM_EXECUCAO"`

> E-mail de aprovação disparado (visível nos logs).

---

### Passo 12 — Iniciar execução do serviço (item)

`Ordens de Serviço > Iniciar execução do serviço (item)`

`PATCH /api/ordens/{{osId}}/itens-servico/{{itemServicoId}}/iniciar`

**Resposta esperada:** `200 OK` com `itensServico[].dataInicioExecucao` preenchido.

> O `{{itemServicoId}}` vem da resposta da criação da OS (`itensServico[0].id`).

---

### Passo 13 — Finalizar execução do serviço (item)

`Ordens de Serviço > Finalizar execução do serviço (item)`

`PATCH /api/ordens/{{osId}}/itens-servico/{{itemServicoId}}/finalizar`

**Resposta esperada:** `200 OK` com `itensServico[].dataFimExecucao` preenchido.

---

### Passo 14 — Avançar status: EM_EXECUCAO → FINALIZADA

`Ordens de Serviço > Avancar status → FINALIZADA`

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta esperada:** `200 OK` com `"status": "FINALIZADA"`

---

### Passo 15 — Registrar pagamento

`Pagamentos > Registrar pagamento da OS`

`POST /api/ordens/{{osId}}/pagamento`

```json
{
  "formaPagamento": "PIX",
  "valor": 211.80
}
```

**Resposta esperada:** `201 Created` — `{{pagamentoId}}` salvo automaticamente.

> Formas aceitas: `DINHEIRO` | `CARTAO_DEBITO` | `CARTAO_CREDITO` | `PIX` | `TRANSFERENCIA`

---

### Passo 16 — Confirmar pagamento (OS avança para ENTREGUE)

`Pagamentos > Confirmar pagamento`

`PATCH /api/pagamentos/{{pagamentoId}}/confirmar`

**Resposta esperada:** `200 OK` com `"status": "PAGO"`

> Ao confirmar, a OS avança **automaticamente** para `ENTREGUE` e o e-mail de confirmação é disparado.

---

### Passo 17 — Verificar OS entregue

`GET /api/ordens/{{osId}}`

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

### Passo 18 — Acompanhar OS pelo número (público, sem token)

`GET /api/ordens/acompanhar/{{osNumero}}`

> Endpoint **sem autenticação** — simula o cliente consultando o andamento pelo número da OS.

---

## Cenário 2 — Adicionar peça ou serviço a uma OS existente

Testa a adição de itens após abertura da OS, disponível nos status `RECEBIDA`, `EM_DIAGNOSTICO`, `AGUARDANDO_APROVACAO` e `EM_EXECUCAO`.

> Execute os Passos 1 a 9 do Cenário 1 para ter uma OS em `EM_DIAGNOSTICO`.

### Adicionar peça

`Ordens de Serviço > Adicionar peça a OS existente`

`POST /api/ordens/{{osId}}/items-peca`

```json
{
  "pecaId": "{{pecaId}}",
  "quantidade": 1
}
```

**Resposta esperada:** `200 OK` com OS atualizada — `itensPeca` e `total` recalculados.

> O estoque é deduzido imediatamente.

### Adicionar serviço

`Ordens de Serviço > Adicionar serviço a OS existente`

`POST /api/ordens/{{osId}}/items-servico`

```json
{
  "servicoId": "{{servicoId}}",
  "observacoes": "Serviço adicional solicitado"
}
```

**Resposta esperada:** `200 OK` com OS atualizada — `itensServico` e `total` recalculados.

### Casos de erro

| Situação | Resposta |
|---|---|
| Peça/serviço já na OS | `422` — "já foi adicionado a esta OS" |
| Estoque insuficiente | `422` — "Estoque insuficiente para a peça" |
| Peça/serviço desativado | `422` — "Peça/Serviço desativado não pode ser adicionado" |
| OS em status inválido (`FINALIZADA`, `ENTREGUE`, `CANCELADA`) | `422` — "Não é possível adicionar em uma ordem com status..." |

---

## Cenário 3 — Cancelamento de OS com devolução de estoque

### Passo 1 — Verificar estoque antes

`GET /api/pecas/{{pecaId}}` — anote `quantidadeEstoque`.

### Passo 2 — Criar uma nova OS

Execute `Criar OS (com serviço e peça)`. O estoque é deduzido na criação.

### Passo 3 — (Opcional) Avançar para EM_DIAGNOSTICO

`PATCH /api/ordens/{{osId}}/avancar`

> Cancelamento permitido em: `RECEBIDA`, `EM_DIAGNOSTICO`, `AGUARDANDO_APROVACAO`.

### Passo 4 — Cancelar

`Ordens de Serviço > Cancelar OS`

`PATCH /api/ordens/{{osId}}/cancelar`

**Resposta esperada:** `200 OK` com `"status": "CANCELADA"` e `dataFechamento` preenchida.

> E-mail de cancelamento disparado (visível nos logs). Estoque devolvido automaticamente.

### Passo 5 — Confirmar devolução do estoque

`GET /api/pecas/{{pecaId}}` — `quantidadeEstoque` deve ter voltado ao valor anterior.

### Casos de erro

| Situação | Endpoint | HTTP | Mensagem |
|---|---|---|---|
| Cancelar OS em `EM_EXECUCAO` | `PATCH /ordens/{id}/cancelar` | `422` | "em execução ou finalizada não pode ser cancelada" |
| Cancelar OS `ENTREGUE` | `PATCH /ordens/{id}/cancelar` | `422` | "já entregue não pode ser cancelada" |
| Avançar OS `CANCELADA` | `PATCH /ordens/{id}/avancar` | `422` | "cancelada não pode ser avançada" |
| Criar OS com estoque insuficiente | `POST /ordens` | `422` | "Estoque insuficiente para a peça" |

---

## Cenário 4 — Alerta de estoque crítico

### Passo 1 — Criar peça com estoque abaixo do mínimo

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

> `quantidadeEstoque (3) < estoqueMinimo (5)` → resposta retorna `"estoqueAbaixoMinimo": true`.

### Passo 2 — Repor estoque via entrada manual

`Peças e Insumos > Entrada de estoque`

`PATCH /api/pecas/{{pecaId}}/estoque`

```json
{
  "operacao": "ENTRADA",
  "quantidade": 10
}
```

**Resposta esperada:** `200 OK` — estoque sobe para `13` e `estoqueAbaixoMinimo` passa a `false`.

### Passo 3 — Registrar saída manual

`Peças e Insumos > Saída de estoque`

`PATCH /api/pecas/{{pecaId}}/estoque`

```json
{
  "operacao": "SAIDA",
  "quantidade": 2
}
```

**Resposta esperada:** `200 OK` — estoque cai para `11`.

> Retorna `422` se a quantidade solicitada for maior que o estoque disponível.

---

## Cenário 5 — Reposição de estoque via Pedido de Compra

Fluxo de aprovação formal antes de atualizar o estoque.

### Passo 1 — Criar pedido de compra

`Pedidos de Compra > Criar pedido de compra`

```json
{
  "pecaId": "{{pecaId}}",
  "quantidadeSolicitada": 20,
  "observacoes": "Estoque crítico — repor urgente"
}
```

**Resposta esperada:** `201 Created` — `{{pedidoCompraId}}` salvo automaticamente.

### Passo 2 — Aprovar pedido

`PATCH /api/pedidos-compra/{{pedidoCompraId}}/aprovar`

Status: `PENDENTE → APROVADO`

### Passo 3 — Receber mercadoria

`PATCH /api/pedidos-compra/{{pedidoCompraId}}/receber?quantidade=20`

Status: `APROVADO → RECEBIDO`

> Estoque da peça **incrementado automaticamente** pela quantidade recebida.

### Passo 4 — (Alternativa) Cancelar pedido

`PATCH /api/pedidos-compra/{{pedidoCompraId}}/cancelar`

Status: `PENDENTE ou APROVADO → CANCELADO`

> Retorna `422` se já estiver `RECEBIDO`.

---

## Cenário 6 — Monitoramento de tempo médio de execução

`Ordens de Serviço > Monitoramento — tempo médio de execução`

`GET /api/ordens/monitoramento/tempo-medio`

```json
{
  "tempoMedioMinutos": 127.5,
  "tempoMedioHoras": 2.13,
  "descricao": "Tempo médio de execução dos serviços finalizados"
}
```

> Calculado sobre os itens de serviço com `dataInicioExecucao` e `dataFimExecucao` preenchidos.
> Retorna `0.0` se ainda não houver itens de serviço finalizados.

---

## Gestão Administrativa — CRUD de Clientes

`Clientes > *`

### Criar — Pessoa Física

`POST /api/clientes`

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

`201 Created` — `{{clienteId}}` salvo automaticamente.

### Criar — Pessoa Jurídica

`POST /api/clientes`

```json
{
  "nome": "Transportes Silva LTDA",
  "tipoPessoa": "PJ",
  "documento": "11.222.333/0001-81",
  "email": "contato@transportes-silva.com.br",
  "telefone": "(11) 3333-4444",
  "endereco": "Av. Industrial, 500 - Guarulhos/SP"
}
```

### Listar todos

`GET /api/clientes` → `200 OK` com array de clientes.

### Buscar por ID

`GET /api/clientes/{{clienteId}}` → `200 OK` com dados do cliente.

### Buscar por CPF/CNPJ

`GET /api/clientes/documento/52998224725` → aceita com ou sem pontuação.

### Atualizar

`PUT /api/clientes/{{clienteId}}` — requer todos os campos obrigatórios:

```json
{
  "nome": "João da Silva Atualizado",
  "tipoPessoa": "PF",
  "documento": "529.982.247-25",
  "email": "joao.novo@email.com",
  "telefone": "(11) 98888-5678",
  "endereco": "Rua Nova, 456 - São Paulo/SP"
}
```

### Desativar (soft delete)

`DELETE /api/clientes/{{clienteId}}` → `204 No Content`

> Cliente marcado como `ativo: false`. Não aparece em listagens, mas permanece no banco.

### Validações

| Situação | HTTP | Detalhe |
|---|---|---|
| Documento duplicado | `409` | CPF ou CNPJ já cadastrado |
| E-mail inválido | `400` | Formato de e-mail inválido |
| `tipoPessoa` ausente | `400` | Campo obrigatório |
| `documento` ausente | `400` | Campo obrigatório |

---

## Gestão Administrativa — CRUD de Veículos

`Veículos > *`

### Criar — Placa padrão

`POST /api/veiculos`

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

`201 Created` — `{{veiculoId}}` salvo automaticamente.

### Criar — Placa Mercosul

`POST /api/veiculos`

```json
{
  "placa": "BRA2E19",
  "marca": "Honda",
  "modelo": "Civic",
  "ano": 2023,
  "cor": "Preto",
  "clienteId": "{{clienteId}}"
}
```

### Listar todos

`GET /api/veiculos` → `200 OK` com array de veículos.

### Buscar por ID

`GET /api/veiculos/{{veiculoId}}`

### Buscar por placa

`GET /api/veiculos/placa/ABC1234`

### Listar veículos de um cliente

`GET /api/veiculos/cliente/{{clienteId}}`

### Atualizar

`PUT /api/veiculos/{{veiculoId}}` — requer todos os campos obrigatórios:

```json
{
  "placa": "ABC1234",
  "marca": "Toyota",
  "modelo": "Corolla XEi",
  "ano": 2022,
  "cor": "Branco",
  "clienteId": "{{clienteId}}"
}
```

### Desativar (soft delete)

`DELETE /api/veiculos/{{veiculoId}}` → `204 No Content`

### Validações

| Situação | HTTP | Detalhe |
|---|---|---|
| Placa duplicada | `409` | Placa já cadastrada |
| Formato de placa inválido | `400` | Deve ser padrão antigo (ABC1234) ou Mercosul (ABC1D23) |
| `clienteId` inválido | `404` | Cliente não encontrado |
| Ano fora do intervalo | `400` | Deve ser entre 1900 e 2027 |

---

## Gestão Administrativa — CRUD de Serviços

`Serviços > *`

### Criar

`POST /api/servicos`

```json
{
  "nome": "Troca de Óleo e Filtro",
  "descricao": "Troca completa do óleo motor com substituição do filtro de óleo",
  "preco": 120.00
}
```

`201 Created` — `{{servicoId}}` salvo automaticamente.

### Listar ativos

`GET /api/servicos`

### Listar todos (incluindo inativos)

`GET /api/servicos/todos`

### Buscar por ID

`GET /api/servicos/{{servicoId}}`

### Atualizar

`PUT /api/servicos/{{servicoId}}`

```json
{
  "nome": "Troca de Óleo e Filtro Completo",
  "descricao": "Troca completa do óleo motor e filtro, com inspeção do arrefecimento",
  "preco": 145.00
}
```

### Vincular insumo (peça necessária para o serviço)

`POST /api/servicos/{{servicoId}}/insumos`

```json
{
  "pecaId": "{{pecaId}}",
  "quantidade": 1
}
```

> Define que este serviço consome essa peça do estoque quando realizado.

### Atualizar quantidade do insumo

`PUT /api/servicos/{{servicoId}}/insumos/{{pecaId}}`

```json
{
  "pecaId": "{{pecaId}}",
  "quantidade": 2
}
```

### Remover insumo do serviço

`DELETE /api/servicos/{{servicoId}}/insumos/{{pecaId}}` → `204 No Content`

### Desativar serviço (soft delete)

`DELETE /api/servicos/{{servicoId}}` → `204 No Content`

> Serviço inativo não pode ser adicionado a novas OS, mas permanece nas OS já criadas.

### Validações

| Situação | HTTP | Detalhe |
|---|---|---|
| `nome` ausente | `400` | Campo obrigatório |
| `preco` ausente ou zero | `400` | Deve ser maior que 0 |

---

## Gestão Administrativa — CRUD de Peças e Insumos com controle de estoque

`Peças e Insumos > *`

### Criar

`POST /api/pecas`

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

`201 Created` — `{{pecaId}}` salvo automaticamente.

> `estoqueAbaixoMinimo: true` quando `quantidadeEstoque < estoqueMinimo`.

### Listar ativas

`GET /api/pecas`

### Listar todas (incluindo inativas)

`GET /api/pecas/todas`

### Buscar por ID

`GET /api/pecas/{{pecaId}}`

### Atualizar dados cadastrais

`PUT /api/pecas/{{pecaId}}`

```json
{
  "codigo": "FILTRO-OLEO-001",
  "nome": "Filtro de Óleo Motor Premium",
  "descricao": "Filtro de óleo premium para motores 1.0 a 2.0",
  "preco": 52.90,
  "estoqueMinimo": 15,
  "unidadeMedida": "UN"
}
```

> `quantidadeEstoque` **não é atualizado** por este endpoint — use o endpoint de movimentação.

### Entrada de estoque (recebimento)

`PATCH /api/pecas/{{pecaId}}/estoque`

```json
{
  "operacao": "ENTRADA",
  "quantidade": 30
}
```

`200 OK` — estoque incrementado. Retorna peça com novo saldo.

### Saída de estoque (acerto/consumo)

`PATCH /api/pecas/{{pecaId}}/estoque`

```json
{
  "operacao": "SAIDA",
  "quantidade": 5
}
```

`200 OK` — estoque decrementado. Retorna `422` se quantidade > estoque disponível.

### Desativar peça (soft delete)

`DELETE /api/pecas/{{pecaId}}` → `204 No Content`

> Peça inativa não pode ser adicionada a novas OS.

### Validações

| Situação | HTTP | Detalhe |
|---|---|---|
| `codigo` duplicado | `409` | Código já cadastrado |
| `codigo` ou `nome` ausente | `400` | Campo obrigatório |
| `preco` zero ou negativo | `400` | Deve ser maior que 0 |
| Saída maior que estoque | `422` | "Estoque insuficiente. Disponível: X, solicitado: Y" |

---

## Gestão Administrativa — Listagem e detalhamento de Ordens de Serviço

`Ordens de Serviço > *`

### Listar todas as OS

`GET /api/ordens` → `200 OK` com array completo.

### Filtrar por status

`GET /api/ordens?status=EM_EXECUCAO`

Valores possíveis: `RECEBIDA` | `EM_DIAGNOSTICO` | `AGUARDANDO_APROVACAO` | `EM_EXECUCAO` | `FINALIZADA` | `ENTREGUE` | `CANCELADA`

### Buscar OS por ID (detalhamento completo)

`GET /api/ordens/{{osId}}`

Resposta inclui:
- Dados do cliente e veículo
- `itensServico` — lista de serviços com preço snapshot e observações
- `itensPeca` — lista de peças com código, nome, quantidade e preço snapshot
- `totalServicos`, `totalPecas`, `total`
- `dataAbertura` e `dataFechamento` (quando encerrada)
- `statusDescricao` — descrição legível do status

### Acompanhar pelo número (sem token)

`GET /api/ordens/acompanhar/OS-20260414-A3F7C2`

> Endpoint público — permite ao cliente consultar o andamento sem login.

---

## Gestão Administrativa — Monitoramento do tempo médio de execução

`Ordens de Serviço > Monitoramento — tempo médio de execução`

`GET /api/ordens/monitoramento/tempo-medio`

```json
{
  "tempoMedioMinutos": 127.5,
  "tempoMedioHoras": 2.13,
  "descricao": "Tempo médio entre abertura e fechamento das OS finalizadas"
}
```

> Calculado sobre todas as OS com `dataFechamento` preenchida (`ENTREGUE`).  
> Retorna `0.0` se nenhuma OS tiver sido encerrada ainda.

Para ter dados no monitoramento, execute o **Cenário 1** completo (até o Passo 14) e então consulte este endpoint.

---

## Referência rápida — Endpoints disponíveis

| Módulo | Método | Endpoint |
|---|---|---|
| Auth | `POST` | `/api/auth/registrar` |
| Auth | `POST` | `/api/auth/login` |
| Clientes | `GET` | `/api/clientes` |
| Clientes | `GET` | `/api/clientes/{id}` |
| Clientes | `GET` | `/api/clientes/documento/{doc}` |
| Clientes | `POST` | `/api/clientes` |
| Clientes | `PUT` | `/api/clientes/{id}` |
| Clientes | `DELETE` | `/api/clientes/{id}` (soft delete) |
| Veículos | `GET` | `/api/veiculos` |
| Veículos | `GET` | `/api/veiculos/{id}` |
| Veículos | `GET` | `/api/veiculos/placa/{placa}` |
| Veículos | `GET` | `/api/veiculos/cliente/{clienteId}` |
| Veículos | `POST` | `/api/veiculos` |
| Veículos | `PUT` | `/api/veiculos/{id}` |
| Veículos | `DELETE` | `/api/veiculos/{id}` (soft delete) |
| Peças | `GET` | `/api/pecas` (ativas) |
| Peças | `GET` | `/api/pecas/todas` (incluindo inativas) |
| Peças | `GET` | `/api/pecas/{id}` |
| Peças | `POST` | `/api/pecas` |
| Peças | `PUT` | `/api/pecas/{id}` |
| Peças | `PATCH` | `/api/pecas/{id}/estoque` |
| Peças | `DELETE` | `/api/pecas/{id}` (soft delete) |
| Serviços | `GET` | `/api/servicos` (ativos) |
| Serviços | `GET` | `/api/servicos/todos` |
| Serviços | `GET` | `/api/servicos/{id}` |
| Serviços | `POST` | `/api/servicos` |
| Serviços | `PUT` | `/api/servicos/{id}` |
| Serviços | `POST` | `/api/servicos/{id}/insumos` |
| Serviços | `PUT` | `/api/servicos/{id}/insumos/{pecaId}` |
| Serviços | `DELETE` | `/api/servicos/{id}/insumos/{pecaId}` |
| Serviços | `DELETE` | `/api/servicos/{id}` (soft delete) |
| Ordens | `GET` | `/api/ordens` |
| Ordens | `GET` | `/api/ordens?status=EM_EXECUCAO` |
| Ordens | `GET` | `/api/ordens/{id}` |
| Ordens | `GET` | `/api/ordens/preparar-abertura?documento={doc}&placa={placa}` |
| Ordens | `POST` | `/api/ordens` |
| Ordens | `POST` | `/api/ordens/{id}/items-peca` |
| Ordens | `POST` | `/api/ordens/{id}/items-servico` |
| Ordens | `PATCH` | `/api/ordens/{id}/itens-servico/{itemId}/iniciar` |
| Ordens | `PATCH` | `/api/ordens/{id}/itens-servico/{itemId}/finalizar` |
| Ordens | `PATCH` | `/api/ordens/{id}/avancar` |
| Ordens | `PATCH` | `/api/ordens/{id}/cancelar` |
| Ordens | `GET` | `/api/ordens/monitoramento/tempo-medio` |
| Ordens | `GET` | `/api/ordens/acompanhar/{numero}` *(sem token)* |
| Pagamentos | `POST` | `/api/ordens/{id}/pagamento` |
| Pagamentos | `GET` | `/api/ordens/{id}/pagamento` |
| Pagamentos | `PATCH` | `/api/pagamentos/{id}/confirmar` |
| Pagamentos | `PATCH` | `/api/pagamentos/{id}/cancelar` |
| Pedidos | `GET` | `/api/pedidos-compra` |
| Pedidos | `GET` | `/api/pedidos-compra?status=PENDENTE` |
| Pedidos | `GET` | `/api/pedidos-compra/{id}` |
| Pedidos | `POST` | `/api/pedidos-compra` |
| Pedidos | `PATCH` | `/api/pedidos-compra/{id}/aprovar` |
| Pedidos | `PATCH` | `/api/pedidos-compra/{id}/receber?quantidade=N` |
| Pedidos | `PATCH` | `/api/pedidos-compra/{id}/cancelar` |

---

## Referência rápida — Status da OS

```
RECEBIDA
   ↓ /avancar
EM_DIAGNOSTICO
   ↓ /avancar                ← /cancelar disponível até aqui
AGUARDANDO_APROVACAO
   ↓ /avancar (cliente aprova)
EM_EXECUCAO                  ← /cancelar NÃO permitido a partir daqui
   ↓ /avancar
FINALIZADA
   ↓ automático ao confirmar pagamento
ENTREGUE
```

## Referência rápida — Códigos HTTP

| Código | Significado |
|---|---|
| `200` | OK |
| `201` | Created — recurso criado |
| `204` | No Content — soft delete realizado |
| `400` | Bad Request — campo inválido ou faltando |
| `401` | Unauthorized — token ausente ou expirado |
| `403` | Forbidden — token válido de usuário inexistente |
| `404` | Not Found — recurso não encontrado |
| `409` | Conflict — unicidade violada (ex: código de peça duplicado) |
| `422` | Unprocessable Entity — regra de negócio violada |
