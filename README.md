# SIASE - Sistema Integrado de Atendimento e Execucao de Servicos

> Pos Tech - Software Architecture | FIAP | Fase 2 - Evolucao da Aplicacao

## Descricao da Solucao

O SIASE e um sistema de gestao de oficina mecanica que gerencia clientes, veiculos, servicos, pecas e ordens de servico. A Fase 2 evolui a aplicacao com:

- **Clean Architecture (Hexagonal)** com 3 modulos Maven independentes
- **APIs REST** para o ciclo completo de ordens de servico
- **Inversao de dependencia** — domain nao depende de frameworks, application nao depende de infra
- **Dockerfile** atualizado para build multi-modulo + healthcheck via Actuator

## Tecnologias

| Tecnologia         | Versao  | Justificativa                                                                 |
|--------------------|---------|-------------------------------------------------------------------------------|
| Java               | 17      | LTS com suporte estendido, records, sealed classes                            |
| Spring Boot        | 3.2.4   | Framework consolidado, ecossistema maduro, convencao sobre configuracao       |
| PostgreSQL         | 16      | Banco relacional robusto, ACID, suporte a JSON, open-source e escalavel       |
| Flyway             | —       | Controle de versao do schema de banco via migrations                          |
| Springdoc OpenAPI  | 2.4.0   | Documentacao automatica da API via anotacoes                                  |
| Docker / Compose   | —       | Ambiente reproduzivel e isolado para dev e producao                           |
| MapStruct          | 1.5.x   | Mapeamento entre entidades JPA e POJOs de dominio em tempo de compilacao      |
| JaCoCo             | 0.8.11  | Cobertura de testes com regra obrigatoria no build (minimo 80%)               |

### Por que PostgreSQL?

PostgreSQL foi escolhido por ser um banco relacional maduro com suporte a ACID, ideal para sistemas transacionais como atendimentos e execucao de servicos. Oferece excelente desempenho, suporte a JSON/JSONB para dados semi-estruturados, extensibilidade e open-source, reduzindo custos de licenca.

## Arquitetura

### Clean Architecture / Hexagonal — 3 Modulos Maven

```
┌──────────────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE (adapters)                   │
│  ┌──────────┐ ┌──────────────┐ ┌──────────┐ ┌────────────┐  │
│  │ web/     │ │ persistence/ │ │ security/│ │ email/      │  │
│  │ REST API │ │ JPA Repos    │ │ JWT      │ │ EmailAdapter│  │
│  └────┬─────┘ └──────┬───────┘ └────┬─────┘ └─────┬──────┘  │
├───────┼───────────────┼──────────────┼───────────────┼────────┤
│       ▼               ▼              │               ▼        │
│  ┌────────────────────────────────┐   │  ┌────────────────┐   │
│  │    APPLICATION (use cases)     │   │  │ Ports (interf) │   │
│  │  CriarOrdemServicoUC           │   │  │ EmailPort      │◄──┤
│  │  ListarOrdensServicoUC         │   │  └────────────────┘   │
│  │  ConsultarStatusOSUC           │   │                       │
│  │  AprovarOrcamentoUC            │   │                       │
│  │  AtualizarStatusViaWebhookUC   │   │                       │
│  │  AvancarStatusUC               │   │                       │
│  │  CancelarOrdemUC               │   │                       │
│  │  AdicionarPecaUC               │   │                       │
│  │  AdicionarServicoUC            │   │                       │
│  │  ConsultarTempoMedioUC         │   │                       │
│  │  IniciarExecucaoItemUC         │   │                       │
│  │  FinalizarExecucaoItemUC       │   │                       │
│  │  PrepararAberturaOSUC          │   │                       │
│  └───────────────┬────────────────┘   │                       │
├──────────────────┼────────────────────┼───────────────────────┤
│                  ▼                    ▼                       │
│  ┌───────────────────────────────────────────────────────┐    │
│  │              DOMAIN (core, zero deps)                  │    │
│  │  Entities, Enums, Port interfaces, Domain services     │    │
│  └───────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

### Estrutura do Projeto

```
15SOAT/
├── pom.xml                        # POM pai (agregador)
├── siase-domain/                   # Entidades POJO, enums, ports, validacoes
├── siase-application/              # Use cases, DTOs, port interfaces
├── siase-infrastructure/           # JPA entities, mappers MapStruct, controllers, security
├── frontend/                       # Frontend Angular
├── k8s/                            # (proxima fase)
├── infra/                          # (proxima fase)
├── .github/                        # (proxima fase)
├── Dockerfile                      # Multi-stage multi-modulo + healthcheck
├── docker-compose.yml              # Dev local (backend + DB)
├── docker-compose.full.yml         # Stack completa (+ frontend Angular)
├── docker-compose.sonarqube.yml    # SonarQube para analise estatica
└── postman/                        # Collection e guia de testes
```

**Regra de dependencia:** `infrastructure → application → domain` (seta sempre aponta para dentro). O compilador impede violacoes de arquitetura.

---

## APIs de Ordem de Servico

### Fluxo de Status

```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → APROVADO → EM_EXECUCAO → FINALIZADA → ENTREGUE
                                      ↑                  ↑
                               cliente recusa      cliente aprova
                               (OS cancelada)      orcamento
```

### Endpoints Principais

#### Ordens de Servico

| Metodo | Endpoint | Descricao | Auth |
|--------|----------|-----------|------|
| POST | `/api/ordens` | Abertura de OS | JWT |
| GET | `/api/ordens?status={status}` | Listar OS (filtro opcional por status) | JWT |
| GET | `/api/ordens/{id}` | Consulta OS por ID | JWT |
| PATCH | `/api/ordens/{id}/avancar` | Avanca status da OS | JWT |
| PATCH | `/api/ordens/{id}/cancelar` | Cancela a OS | JWT |
| POST | `/api/ordens/{id}/items-peca` | Adiciona peca a OS | JWT |
| POST | `/api/ordens/{id}/items-servico` | Adiciona servico a OS | JWT |
| PATCH | `/api/ordens/{id}/itens-servico/{itemId}/iniciar` | Inicia execucao de item | JWT |
| PATCH | `/api/ordens/{id}/itens-servico/{itemId}/finalizar` | Finaliza execucao de item | JWT |
| GET | `/api/ordens/preparar-abertura?documento={doc}` | Busca cliente/veiculos para abertura | JWT |
| GET | `/api/ordens/monitoramento/tempo-medio` | Tempo medio de execucao dos servicos | JWT |
| GET | `/api/ordens/acompanhar/{numero}` | Acompanhamento (publico) | — |
| PATCH | `/api/ordens/acompanhar/{numero}/aprovar-orcamento` | Aprovar orcamento (publico) | — |
| PATCH | `/api/ordens/acompanhar/{numero}/recusar-orcamento` | Recusar orcamento (publico) | — |
| POST | `/api/ordens/webhook/status` | Atualizacao via webhook externo | Token |

### Seguranca

- JWT aplicado nas rotas administrativas (nao nas publicas)
- Validacao de CPF/CNPJ via biblioteca **caelum-stella**
- Suporte a placa **Mercosul** (ABC1D23) e **antiga** (ABC-1234)
- Senhas com hash BCrypt
- Usuario padrao: `mecanico` / senha configurada via `MECANICO_PASSWORD`

---

## Execucao Local

### Pre-requisitos

- Docker e Docker Compose instalados
- Java 17+ (para rodar sem Docker)
- Maven 3.9+ (para rodar sem Docker)

---

### Opcao 1 — Back-end + Banco (avaliacao da API)

Sobe apenas o banco de dados e a aplicacao Spring Boot. Ideal para avaliar os endpoints via Swagger ou Postman.

```bash
docker compose up --build
```

| Servico | URL |
|---------|-----|
| API REST | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/api/swagger-ui.html` |
| Health Check | `http://localhost:8080/api/actuator/health` |

---

### Opcao 2 — Stack completa: Back-end + Front-end + Banco

Sobe a stack inteira incluindo a interface web Angular. Permite visualizar e operar o sistema completo.

```bash
docker compose -f docker-compose.full.yml up --build
```

| Servico | URL |
|---------|-----|
| Interface Web | `http://localhost:4200` |
| API REST | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/api/swagger-ui.html` |

---

### Opcao 3 — Desenvolvimento local (sem Docker para a aplicacao)

Util para desenvolvimento com hot-reload. Sobe somente o banco via Docker e roda a aplicacao localmente.

```bash
# 1. Suba o PostgreSQL
docker compose up postgres

# 2. Configure as variaveis de ambiente
cp .env.example .env

# 3. Compile todos os modulos
./mvnw clean install -DskipTests

# 4. Execute a aplicacao
./mvnw spring-boot:run -pl siase-infrastructure
```

### Variaveis de Ambiente

| Variavel            | Padrao         | Descricao                        |
|---------------------|----------------|----------------------------------|
| `DB_HOST`           | `localhost`    | Host do banco de dados           |
| `DB_PORT`           | `5432`         | Porta do PostgreSQL              |
| `DB_NAME`           | `siase_db`     | Nome do banco                    |
| `DB_USER`           | `siase_user`   | Usuario do banco                 |
| `DB_PASSWORD`       | `siase_pass`   | Senha do banco                   |
| `SERVER_PORT`       | `8080`         | Porta da aplicacao               |
| `JWT_SECRET`        | *(ver .env.example)* | Chave HMAC para assinar tokens |
| `JWT_EXPIRATION_MS` | `3600000`      | Expiracao do token (ms) — 1h    |
| `MECANICO_PASSWORD` | `mecanico123`  | Senha do usuario padrao          |
| `WEBHOOK_TOKEN`     | *(ver .env.example)* | Token para webhook externo   |

---

## Primeiros passos (autenticacao)

Todos os endpoints administrativos exigem JWT. Apos subir a aplicacao:

```bash
# 1. Registrar usuario (sem autenticacao)
POST http://localhost:8080/api/auth/registrar
{"username": "admin", "password": "Admin@123"}

# 2. Fazer login e obter o token
POST http://localhost:8080/api/auth/login
{"username": "admin", "password": "Admin@123"}
# → retorna {"token": "eyJ..."}

# 3. Usar o token nas chamadas seguintes
Authorization: Bearer eyJ...
```

## Explorando a API

A forma recomendada para explorar e testar os endpoints e via **Swagger UI**, disponivel assim que a aplicacao estiver no ar:

```
http://localhost:8080/api/swagger-ui.html
```

O Swagger documenta todos os endpoints, exibe os schemas de request/response e permite executar chamadas diretamente pelo navegador — basta clicar em **Authorize**, informar o Bearer token obtido no login e chamar qualquer endpoint.

A pasta `postman/` tambem disponibiliza:

- `SIASE.postman_collection.json` — colecao com todos os endpoints pre-configurados
- `GUIA_DE_TESTES.md` — roteiro com cenarios de teste (ciclo completo de OS, cancelamento, controle de estoque, CRUD administrativo)

## Documentacao da API

Com a aplicacao rodando, acesse:

- **Swagger UI:** `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api/v3/api-docs`

## Relatorio SonarQube

O script `scripts/sonar_pdf_report.py` gera um relatorio PDF com as metricas do SonarQube. O arquivo gerado e salvo em:

```
scripts/target/sonar/relatorio_sonarqube.pdf
```

### Pre-requisitos

- Python 3.x com as dependencias: `fpdf2`, `matplotlib`, `requests`
- SonarQube rodando em `http://localhost:9000` com o projeto `br.com.fiap:siase` ja analisado

### Como gerar

```bash
# 1. Suba o SonarQube
docker compose -f docker-compose.sonarqube.yml up -d

# 2. Execute o scan Maven
./mvnw sonar:sonar -Dsonar.token=SEU_TOKEN

# 3. Gere o PDF
cd scripts
python sonar_pdf_report.py
```

O PDF sera criado automaticamente em `scripts/target/sonar/` (o diretorio e criado se nao existir).

## Testes

```bash
# Rodar todos os testes
./mvnw test

# Rodar com relatorio de cobertura
./mvnw test jacoco:report
```

> Os testes estao organizados por modulo seguindo Clean Architecture:
> - `siase-domain/src/test/` — testes unitarios de regras de negocio (OrdemDeServico, Peca)
> - `siase-application/src/test/` — testes unitarios dos use cases com mocks dos ports
> - `siase-infrastructure/src/test/` — testes de controller com MockMvc e integracao com H2
>
> Cobertura minima de 80% de linhas configurada como regra obrigatoria do build via JaCoCo. Evidencias de TDD documentadas nos fluxos criticos de OS.

## Docker

- **Multi-stage build:** estagio de build com Maven + estagio runtime com JRE
- **Non-root user:** container executa como `appuser`
- **Healthcheck:** `curl` no endpoint `/api/actuator/health` com Spring Boot Actuator
- **docker-compose:** healthcheck no PostgreSQL com `depends_on` aguardando `service_healthy`

## Links

- **Swagger UI:** `http://localhost:8080/api/swagger-ui.html`
- **Postman Collection:** `postman/SIASE.postman_collection.json`
- **Link para video demonstrativo:** [YouTube / Vimeo]
