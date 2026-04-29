# Guia de Testes — SIASE via Postman/Insomnia

> **Pré-requisitos**
> - Aplicação rodando em `http://localhost:8080`
> - Coleção `SIASE.postman_collection.json` importada
> - PostgreSQL ativo e migrations aplicadas (Flyway roda automaticamente ao iniciar)
> - Para resetar os dados entre testes: execute `postman/reset_base.sql` e limpe a variável `{{jwtToken}}`

---

## Primeiro uso — Criar usuário do sistema

Antes de executar qualquer cenário, registre um usuário para obter acesso autenticado.

**Requisição:** `Autenticação > Registrar usuário`

`POST /api/auth/registrar`

```json
{
  "username": "atendente1",
  "password": "Atend@2024"
}
```

**Resposta esperada:** `201 Created`

> Execute **apenas uma vez**. Se o usuário já existir, vá direto para o Login no Cenário 1.

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

## Cenário 1 — Criação da Ordem de Serviço (fluxo completo)

Fluxo principal do sistema: da identificação do cliente até a entrega do veículo com pagamento confirmado.  
Execute os passos **na ordem abaixo** usando a pasta **🔄 Fluxo Principal** da collection.

---

### Passo 1 — Autenticação

**Requisição:** `Autenticação > Login`

```json
{
  "username": "atendente1",
  "password": "Atend@2024"
}
```

**Resposta esperada:** `200 OK` — token salvo automaticamente em `{{jwtToken}}`.

> Se o usuário ainda não existir, execute `Autenticação > Registrar usuário` antes.

---

### Passo 2 — Identificação do cliente por CPF/CNPJ

**Requisição:** `🔄 Fluxo Principal > Passo 2 — Identificar cliente por CPF/CNPJ`

`GET /api/ordens/preparar-abertura?documento=52998224725&placa=ABC1234`

**Resposta esperada:** `200 OK`

```json
{
  "cliente": { "id": "...", "nome": "João da Silva", "documento": "52998224725" },
  "veiculoSelecionado": { "id": "...", "placa": "ABC1234", "modelo": "Corolla", "ano": 2022 },
  "prontoParaAbertura": true
}
```

`{{clienteId}}` e `{{veiculoId}}` salvos automaticamente.

> Valida que a placa pertence ao cliente identificado pelo CPF/CNPJ antes de abrir a OS.  
> Se o cliente não existir, execute o **Passo 3**. Se o veículo não existir, execute o **Passo 4**.

---

### Passo 3 — Cadastro do cliente (se não existir)

**Requisição:** `🔄 Fluxo Principal > Passo 3 — Criar cliente`

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

### Passo 4 — Cadastro do veículo (placa, marca, modelo, ano)

**Requisição:** `🔄 Fluxo Principal > Passo 4 — Cadastrar veículo`

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

> Aceita placa padrão (`ABC1234`) ou Mercosul (`ABC1D23`).

---

### Passo 5 — Preparar catálogo (peças e serviços)

Execute **"Passo 5a — Criar peça"** (ex.: Filtro de Óleo Motor, R$ 45,90) e  
**"Passo 5b — Criar serviço"** (ex.: Troca de Óleo, R$ 120,00 — ou Alinhamento e Balanceamento, R$ 180,00).

`{{pecaId}}` e `{{servicoId}}` salvos automaticamente.

> Pule este passo se os itens já existirem no catálogo.

---

### Passo 6 — Inclusão dos serviços solicitados e peças — orçamento gerado automaticamente

**Requisição:** `🔄 Fluxo Principal > Passo 6 — Criar OS`

```json
{
  "clienteId": "{{clienteId}}",
  "veiculoId": "{{veiculoId}}",
  "observacoes": "Cliente relata barulho ao frear e óleo baixo no painel",
  "itensServico": [
    { "servicoId": "{{servicoId}}", "observacoes": "Usar óleo sintético 5W30" }
  ],
  "itensPeca": [
    { "pecaId": "{{pecaId}}", "quantidade": 1 }
  ]
}
```

**Resposta esperada:** `201 Created`

```json
{
  "status": "RECEBIDA",
  "totalServicos": 120.00,
  "totalPecas": 45.90,
  "total": 165.90
}
```

`{{osId}}` e `{{osNumero}}` salvos automaticamente.

> O **orçamento é gerado automaticamente** com base nos preços do catálogo no momento da criação.  
> O estoque das peças é deduzido automaticamente.  
> Serviços e peças podem ser adicionados até o status `EM_DIAGNOSTICO`.

---

### Passo 7 — (Opcional) Inclusão de peças e insumos adicionais

**Requisição:** `🔄 Fluxo Principal > Passo 7 — Adicionar peça extra`

`POST /api/ordens/{{osId}}/items-peca`

```json
{ "pecaId": "{{pecaId}}", "quantidade": 1 }
```

O total é **recalculado automaticamente** após a adição.

---

### Passo 8 — Avançar para EM_DIAGNOSTICO

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta:** `200 OK` com `"status": "EM_DIAGNOSTICO"`

---

### Passo 9 — Envio do orçamento ao cliente para aprovação

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta:** `200 OK` com `"status": "AGUARDANDO_APROVACAO"`

> E-mail disparado ao cliente com o link e o total do orçamento (visível nos logs com prefixo `[EMAIL]`).

---

### Passo 10 — Consulta do cliente via API (sem token)

**Requisição:** `🔄 Fluxo Principal > Passo 10 — Cliente consulta o andamento da OS`

`GET /api/ordens/acompanhar/{{osNumero}}`

**Endpoint público — não exige JWT.**

**Resposta:** `200 OK` com todos os detalhes: status, serviços, peças, totais.

> Simula o cliente acompanhando o progresso pelo número recebido no e-mail.

---

### Passo 11 — Cliente aprova o orçamento (sem token)

**Requisição:** `🔄 Fluxo Principal > Passo 11 — Cliente APROVA o orçamento`

`PATCH /api/ordens/acompanhar/{{osNumero}}/aprovar-orcamento`

**Endpoint público — não exige JWT.**

**Resposta:** `200 OK` com `"status": "APROVADO"`

> E-mail de confirmação disparado ao cliente (log no console).

**Alternativa — recusar:**  
`PATCH /api/ordens/acompanhar/{{osNumero}}/recusar-orcamento`  
Status muda para `CANCELADA`. Estoque das peças devolvido automaticamente.

---

### Passo 12 — Avançar para EM_EXECUCAO (mecânico inicia)

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta:** `200 OK` com `"status": "EM_EXECUCAO"`

> Requer que o cliente tenha aprovado o orçamento (status `APROVADO`).

---

### Passo 13 — Iniciar e finalizar execução do serviço

**Iniciar:**  
`PATCH /api/ordens/{{osId}}/itens-servico/{{itemServicoId}}/iniciar`  
Registra `dataInicioExecucao` no item de serviço.

**Finalizar:**  
`PATCH /api/ordens/{{osId}}/itens-servico/{{itemServicoId}}/finalizar`  
Registra `dataFimExecucao` no item de serviço.

---

### Passo 14 — Avançar para FINALIZADA

`PATCH /api/ordens/{{osId}}/avancar`

**Resposta:** `200 OK` com `"status": "FINALIZADA"`

---

### Passo 15 — Registrar e confirmar pagamento (→ ENTREGUE)

**Registrar:**  
`POST /api/ordens/{{osId}}/pagamento`

```json
{ "formaPagamento": "PIX", "valor": 165.90 }
```

`{{pagamentoId}}` salvo automaticamente.  
Formas aceitas: `DINHEIRO` | `CARTAO_DEBITO` | `CARTAO_CREDITO` | `PIX` | `TRANSFERENCIA`

**Confirmar:**  
`PATCH /api/pagamentos/{{pagamentoId}}/confirmar`

**Resposta:** `200 OK` — OS avança automaticamente para `ENTREGUE`. E-mail de pagamento disparado. ✅

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

Valores possíveis: `RECEBIDA` | `EM_DIAGNOSTICO` | `AGUARDANDO_APROVACAO` | `APROVADO` | `EM_EXECUCAO` | `FINALIZADA` | `ENTREGUE` | `CANCELADA`

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
| Ordens | `PATCH` | `/api/ordens/acompanhar/{numero}/aprovar-orcamento` *(sem token)* |
| Ordens | `PATCH` | `/api/ordens/acompanhar/{numero}/recusar-orcamento` *(sem token)* |
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
   ↓ /avancar                        ← /cancelar disponível até aqui (inclui APROVADO)
AGUARDANDO_APROVACAO
   ↓ /acompanhar/{numero}/aprovar-orcamento   (cliente aprova — sem token)
APROVADO
   ↓ /avancar                        (mecânico inicia execução)
EM_EXECUCAO                           ← /cancelar NÃO permitido a partir daqui
   ↓ /avancar
FINALIZADA
   ↓ automático ao confirmar pagamento
ENTREGUE

Alternativa após AGUARDANDO_APROVACAO:
   ↓ /acompanhar/{numero}/recusar-orcamento   (cliente recusa — sem token)
CANCELADA
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
