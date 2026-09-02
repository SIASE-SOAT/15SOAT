# SIASE - Sistema Integrado de Atendimento e Execucao de Servicos

> Pos Tech - Software Architecture | FIAP | Fase 3 — Seguranca, Observabilidade e Operacao Corporativa

## Descricao da Solucao

O SIASE e um sistema de gestao de oficina mecanica que gerencia clientes, veiculos, servicos, pecas e ordens de servico. Este repositorio contem a aplicacao principal (backend Spring Boot + frontend Angular), evoluida ao longo de 3 fases:

- **Fase 1:** MVP com DDD, APIs REST, autenticacao JWT, controle de estoque e fluxo completo de OS
- **Fase 2:** Refatoracao para Arquitetura Hexagonal, Kubernetes, Terraform e CI/CD automatizado
- **Fase 3:** API Gateway com Lambda Authorizer, observabilidade completa (Prometheus + Grafana + Loki), EKS gerenciado na AWS e estrutura de 4 repositorios independentes com CI/CD

## Tecnologias

| Tecnologia         | Versao  | Justificativa                                                                 |
|--------------------|---------|-------------------------------------------------------------------------------|
| Java               | 17      | LTS com suporte estendido, records, sealed classes                            |
| Spring Boot        | 3.2.4   | Framework consolidado, ecossistema maduro, convencao sobre configuracao       |
| Angular            | 21      | Framework frontend reativo, integrado via proxy reverso Nginx                 |
| PostgreSQL         | 16      | Banco relacional robusto, ACID, suporte a JSON, open-source e escalavel       |
| Flyway             | —       | Controle de versao do schema de banco via migrations versionadas              |
| Springdoc OpenAPI  | 2.4.0   | Documentacao automatica da API via anotacoes                                  |
| Micrometer         | —       | Exposicao de metricas customizadas via `/actuator/prometheus`                 |
| Docker / Compose   | —       | Ambiente reproduzivel e isolado para desenvolvimento local                    |
| MapStruct          | 1.5.x   | Mapeamento entre entidades JPA e POJOs de dominio em tempo de compilacao      |
| JaCoCo             | 0.8.11  | Cobertura de testes com regra obrigatoria no build (minimo 80%)               |
| Kubernetes (EKS)   | 1.30    | Orquestracao de containers em cluster gerenciado na AWS                       |
| Terraform          | 1.7+    | Infraestrutura como codigo — gerencia recursos K8s de forma declarativa       |
| GitHub Actions     | —       | Pipeline CI/CD com build, testes, push de imagem e deploy automatizado        |

### Por que PostgreSQL?

PostgreSQL foi escolhido por ser um banco relacional maduro com suporte a ACID, ideal para sistemas transacionais como atendimentos e execucao de servicos. Oferece excelente desempenho, suporte a JSON/JSONB para dados semi-estruturados, extensibilidade e e open-source, reduzindo custos de licenca. Na Fase 3, o banco e provisionado como RDS gerenciado com criptografia KMS e senha via Secrets Manager.

## Arquitetura

### Visao Geral — Fase 3

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
    consulta RDS              ▼
    emite JWT        ┌─────────────────┐
                     │   EKS (AWS)     │
                     │  namespace siase│
                     │  2-4 replicas   │
                     │  HPA CPU 70%    │
                     └────────┬────────┘
                              │
              ┌───────────────┼───────────────────┐
              │               │                   │
              ▼               ▼                   ▼
       RDS PostgreSQL   Prometheus +        Grafana Alloy
       (subnet privada)  Grafana +           → Loki
                         Alertmanager
```

### Clean Architecture / Hexagonal — 3 Modulos Maven

```
┌──────────────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE (adapters)                   │
│  ┌──────────┐ ┌──────────────┐ ┌──────────┐ ┌────────────┐  │
│  │ web/     │ │ persistence/ │ │ security/│ │observability│  │
│  │ REST API │ │ JPA Repos    │ │ JWT      │ │ Micrometer  │  │
│  └────┬─────┘ └──────┬───────┘ └────┬─────┘ └─────┬──────┘  │
├───────┼───────────────┼──────────────┼───────────────┼────────┤
│       ▼               ▼              │               ▼        │
│  ┌────────────────────────────────┐   │  ┌────────────────┐   │
│  │    APPLICATION (use cases)     │   │  │ Ports (interf) │   │
│  │  CriarOrdemServicoUC           │   │  │ EmailPort      │◄──┤
│  │  ListarOrdensServicoUC         │   │  │ ObservabilityPort   │
│  │  ConsultarStatusOSUC           │   │  └────────────────┘   │
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

**Regra de dependencia:** `infrastructure → application → domain`. O compilador impede violacoes de arquitetura.

### Estrutura do Projeto

```
15SOAT/
├── pom.xml                          # POM pai (agregador)
├── siase-domain/                    # Entidades POJO, enums, ports, validacoes
├── siase-application/               # Use cases, DTOs, port interfaces
├── siase-infrastructure/            # JPA entities, mappers MapStruct, controllers, security
├── frontend/                        # Frontend Angular 21 + Nginx
├── k8s/                             # Manifestos Kubernetes (EKS)
│   ├── namespace.yaml               # Namespace siase
│   ├── configmap.example.yaml       # Exemplo de variaveis nao-sensiveis
│   ├── secret.example.yaml          # Template de secrets
│   ├── kind-cluster.yaml            # Configuracao do cluster Kind (uso local/Fase 2)
│   ├── app-deployment.yaml          # Deployment da aplicacao (2 replicas)
│   ├── app-service.yaml             # Service da aplicacao
│   ├── app-metrics-service.yaml     # Service de management (porta 8081)
│   ├── frontend-deployment.yaml     # Deployment do frontend Angular
│   ├── frontend-service.yaml        # Service do frontend
│   ├── hpa.yaml                     # HPA: 2-4 replicas, CPU target 70%
│   └── servicemonitor.yaml          # ServiceMonitor para Prometheus
├── infra/                           # Terraform local (Kind/VPS — Fase 2)
├── docs/                            # Documentacao arquitetural
│   ├── diagramas-sequencia.md       # Diagramas de sequencia dos fluxos principais
│   ├── adr/                         # Architecture Decision Records
│   └── rfc/                         # Request for Comments
├── .github/
│   └── workflows/
│       ├── ci.yml                   # CI em Pull Requests
│       ├── build-test.yml           # Workflow reutilizavel de build e testes
│       └── deploy-prod.yml          # Deploy automatizado na main
├── scripts/
│   └── sonar_pdf_report.py          # Geracao de relatorio PDF do SonarQube
├── Dockerfile                       # Multi-stage multi-modulo + JVM container flags
├── docker-compose.yml               # Dev local (backend + DB)
├── docker-compose.full.yml          # Stack completa (+ frontend Angular)
├── docker-compose.sonarqube.yml     # SonarQube para analise estatica
└── postman/                         # Collection e guia de testes
```

---

## Estrutura de Repositorios (Fase 3)

O projeto e organizado em 4 repositorios independentes, cada um com CI/CD proprio:

| Repositorio          | Descricao                                                        |
|----------------------|------------------------------------------------------------------|
| `15SOAT`             | Aplicacao principal (Spring Boot + Angular) + manifestos K8s     |
| `siase-auth-lambda`  | Lambda de autenticacao por CPF, Authorizer e API Gateway         |
| `siase-infra-k8s`    | EKS, VPC, observabilidade (Prometheus + Grafana + Loki)          |
| `siase-infra-database` | RDS PostgreSQL gerenciado com KMS e Secrets Manager            |

**Ordem de aplicacao:**
1. `siase-infra-k8s` — cria VPC, EKS e publica parametros SSM
2. `siase-infra-database` — cria RDS consumindo SSM do passo 1
3. `siase-auth-lambda` — cria Lambdas e API Gateway consumindo SSM dos passos 1 e 2
4. `15SOAT` — aplica manifestos K8s no cluster criado no passo 1

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

#### Autenticacao Administrativa

| Metodo | Endpoint | Descricao | Auth |
|--------|----------|-----------|------|
| POST | `/api/auth/registrar` | Registrar novo usuario | — |
| POST | `/api/auth/login` | Login e obtencao do JWT | — |

#### Autenticacao de Cliente (via API Gateway + Lambda)

| Metodo | Endpoint | Descricao | Auth |
|--------|----------|-----------|------|
| POST | `/auth/token` | Autenticar cliente por CPF e obter JWT | — |

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
| GET | `/api/ordens/acompanhar/{numero}` | Acompanhamento publico da OS | — |
| PATCH | `/api/ordens/acompanhar/{numero}/aprovar-orcamento` | Aprovar orcamento (publico) | — |
| PATCH | `/api/ordens/acompanhar/{numero}/recusar-orcamento` | Recusar orcamento (publico) | — |
| POST | `/api/ordens/webhook/status` | Atualizacao via webhook externo | Token |

#### Gestao Administrativa

| Recurso | Endpoints | Auth |
|---------|-----------|------|
| Clientes | `GET/POST /api/clientes`, `GET/PUT/DELETE /api/clientes/{id}` | JWT |
| Veiculos | `GET/POST /api/veiculos`, `GET/PUT/DELETE /api/veiculos/{id}` | JWT |
| Servicos | `GET/POST /api/servicos`, `GET/PUT/DELETE /api/servicos/{id}` | JWT |
| Pecas | `GET/POST /api/pecas`, `GET/PUT/DELETE /api/pecas/{id}` | JWT |
| Pedidos de Compra | `GET/POST /api/pedidos-compra`, `PATCH /api/pedidos-compra/{id}/receber` | JWT |
| Agendamentos | `GET/POST /api/agendamentos`, `GET/PATCH /api/agendamentos/{id}` | JWT |
| Pagamentos | `POST /api/pagamentos`, `GET /api/pagamentos/{ordemId}` | JWT |

### Seguranca

- JWT aplicado nas rotas administrativas (nao nas publicas)
- Autenticacao de clientes via CPF delegada ao API Gateway + Lambda (Fase 3)
- Validacao de CPF/CNPJ via biblioteca **caelum-stella**
- Suporte a placa **Mercosul** (ABC1D23) e **antiga** (ABC-1234)
- Senhas com hash BCrypt
- Usuario padrao: `mecanico` / senha configurada via `MECANICO_PASSWORD`
- Logs estruturados JSON com `correlationId` via `CorrelationIdFilter`

---

## Execucao Local

### Pre-requisitos

- Docker e Docker Compose instalados
- Java 17+ (para rodar sem Docker)
- Maven 3.9+ (para rodar sem Docker)
- Node.js 20+ e npm 10+ (para o frontend sem Docker)

---

### Opcao 1 — Back-end + Banco

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

Sobe a stack inteira incluindo a interface web Angular.

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

| Variavel            | Padrao               | Descricao                        |
|---------------------|----------------------|----------------------------------|
| `DB_HOST`           | `localhost`          | Host do banco de dados           |
| `DB_PORT`           | `5432`               | Porta do PostgreSQL              |
| `DB_NAME`           | `siase_db`           | Nome do banco                    |
| `DB_USER`           | `siase_user`         | Usuario do banco                 |
| `DB_PASSWORD`       | `siase_pass`         | Senha do banco                   |
| `SERVER_PORT`       | `8080`               | Porta da aplicacao               |
| `JWT_SECRET`        | *(ver .env.example)* | Chave HMAC para assinar tokens   |
| `JWT_EXPIRATION_MS` | `3600000`            | Expiracao do token (ms) — 1h    |
| `MECANICO_PASSWORD` | `mecanico123`        | Senha do usuario padrao          |
| `WEBHOOK_TOKEN`     | *(ver .env.example)* | Token para webhook externo       |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Origens permitidas pelo CORS |

---

## Primeiros Passos (Autenticacao)

Todos os endpoints administrativos exigem JWT. Apos subir a aplicacao:

```bash
# 1. Registrar usuario
POST http://localhost:8080/api/auth/registrar
{"username": "admin", "password": "Admin@123"}

# 2. Fazer login e obter o token
POST http://localhost:8080/api/auth/login
{"username": "admin", "password": "Admin@123"}
# retorna {"token": "eyJ..."}

# 3. Usar o token nas chamadas seguintes
Authorization: Bearer eyJ...
```

## Explorando a API

```
http://localhost:8080/api/swagger-ui.html
```

A pasta `postman/` disponibiliza:

- `SIASE.postman_collection.json` — colecao com todos os endpoints pre-configurados
- `GUIA_DE_TESTES.md` — roteiro com cenarios de teste (ciclo completo de OS, cancelamento, controle de estoque, CRUD administrativo)

---

## Observabilidade (Fase 3)

A aplicacao expoe metricas customizadas via Micrometer, coletadas pelo Prometheus e visualizadas no Grafana:

| Metrica | Tipo | Descricao |
|---------|------|-----------|
| `siase_ordens_servico_criadas_total` | Counter | Volume de OS criadas |
| `siase_ordem_servico_tempo_status_seconds` | Timer (histogram) | Tempo por status da OS |
| `siase_execucao_item_iniciadas_total` | Counter | Itens de servico iniciados |
| `siase_execucao_item_tempo_seconds` | Timer (histogram) | Tempo de execucao de itens |
| `siase_falhas_integracao_total` | Counter | Falhas em integracoes externas |

**Endpoints de management (porta 8081):**

| Endpoint | Descricao |
|----------|-----------|
| `/actuator/health/readiness` | Probe de readiness do K8s |
| `/actuator/health/liveness` | Probe de liveness do K8s |
| `/actuator/prometheus` | Metricas para o Prometheus |

**Logs estruturados:** todos os logs sao emitidos em JSON com os campos `correlationId` e `subject`, promovidos a labels pelo Grafana Alloy para correlacao no Loki.

---

## Infraestrutura e Deploy

### Arquitetura de Deploy — Fase 3 (AWS EKS)

```
┌──────────────────────────────────────────────────────────────┐
│                       GitHub Actions                          │
│                                                              │
│  push → main   ┌─────────────────┐                           │
│  ───────────►  │  build-and-test │  ubuntu-latest             │
│                │  mvn clean verify                            │
│                └────────┬────────┘                            │
│                         │ ok                                  │
│                ┌────────▼────────┐                            │
│                │docker-build-push│  ubuntu-latest             │
│                │ push → ECR      │                            │
│                └────────┬────────┘                            │
│                         │ image_tag (git SHA)                 │
│                ┌────────▼──────────────────┐                  │
│                │        deploy             │  ubuntu-latest   │
│                │ 1. AWS OIDC auth          │                  │
│                │ 2. kubectl apply (k8s/)   │                  │
│                │ 3. kubectl rollout status │                  │
│                │ 4. curl /health           │                  │
│                └───────────────┬───────────┘                  │
└───────────────────────────────────────────────────────────────┘
                                 │
┌────────────────────────────────▼──────────────────────────────┐
│                    AWS — us-east-1                             │
│                                                               │
│   ┌───────────────────────────────────────────────────────┐   │
│   │              Amazon EKS (K8s 1.30)                     │   │
│   │   Managed node group · 2 AZs · namespace: siase        │   │
│   │                                                        │   │
│   │   ┌──────────────────────────────────────────────┐    │   │
│   │   │  siase-app: 2-4 replicas (HPA CPU 70%)        │    │   │
│   │   │  siase-frontend: Angular + Nginx              │    │   │
│   │   └──────────────────────────────────────────────┘    │   │
│   │                                                        │   │
│   │   monitoring namespace:                                │   │
│   │   Prometheus · Grafana · Alertmanager · Loki · Alloy   │   │
│   └───────────────────────────────────────────────────────┘   │
│                                                               │
│   RDS PostgreSQL 16 (subnet privada, KMS, Secrets Manager)    │
│   API Gateway HTTP API → Lambda Token / Authorizer            │
└───────────────────────────────────────────────────────────────┘
```

### Deploy em Kubernetes (EKS)

O deploy e feito automaticamente pelo CI/CD a cada push na `main`. O workflow le os segredos do Secrets Manager e os parametros do SSM, cria o ConfigMap e o Secret no cluster, aplica os manifestos e aguarda o DNS do Load Balancer ser publicado. Para aplicar manualmente:

```bash
# 1. Configurar kubeconfig
aws eks update-kubeconfig --region us-east-1 --name siase-production

# 2. Aplicar namespace
kubectl apply -f k8s/namespace.yaml

# 3. Criar ConfigMap e Secret com valores reais
kubectl create configmap siase-config \
  --namespace siase \
  --from-literal="DB_HOST=<endpoint-rds>" \
  --from-literal="DB_PORT=5432" \
  --from-literal="DB_NAME=siase" \
  --from-literal="SERVER_PORT=8080" \
  --from-literal="MANAGEMENT_SERVER_PORT=8081" \
  --from-literal="JWT_EXPIRATION_MS=3600000" \
  --from-literal="JWT_ISSUER=siase-auth" \
  --from-literal="CORS_ALLOWED_ORIGINS=http://localhost:4200" \
  --dry-run=client -o yaml | kubectl apply --server-side -f -

kubectl create secret generic siase-secret \
  --namespace siase \
  --from-literal="DB_USER=<usuario>" \
  --from-literal="DB_PASSWORD=<senha>" \
  --from-literal="JWT_SECRET=<segredo>" \
  --from-literal="WEBHOOK_TOKEN=<token>" \
  --from-literal="MECANICO_PASSWORD=<senha>" \
  --dry-run=client -o yaml | kubectl apply --server-side -f -

# 4. Aplicar a aplicacao (substituir placeholders de imagem antes)
sed -i "s#IMAGE_TAG#<git-sha>#g; s#ECR_REGISTRY#<registry>#g; s#ECR_REPOSITORY#<repo>#g" k8s/app-deployment.yaml
kubectl apply \
  -f k8s/app-deployment.yaml \
  -f k8s/app-service.yaml \
  -f k8s/app-metrics-service.yaml \
  -f k8s/hpa.yaml \
  -f k8s/servicemonitor.yaml

# 5. Verificar
kubectl get pods -n siase
kubectl get hpa -n siase
```

### Manifestos Kubernetes

| Manifesto | Descricao |
|-----------|-----------|
| `namespace.yaml` | Namespace `siase` |
| `configmap.example.yaml` | Exemplo de variaveis nao-sensiveis (ConfigMap criado pelo CI com valores reais) |
| `secret.example.yaml` | Template de secrets (Secret criado pelo CI com valores do Secrets Manager) |
| `kind-cluster.yaml` | Configuracao do cluster Kind para uso local (Fase 2) |
| `app-deployment.yaml` | Deployment da aplicacao (2 replicas, probes, recursos) |
| `app-service.yaml` | Service da aplicacao (LoadBalancer no EKS) |
| `app-metrics-service.yaml` | Service de management na porta 8081 |
| `frontend-deployment.yaml` | Deployment do frontend Angular |
| `frontend-service.yaml` | Service do frontend |
| `hpa.yaml` | HPA: 2-4 replicas, CPU target 70% |
| `servicemonitor.yaml` | ServiceMonitor para coleta pelo Prometheus |

### Pipeline CI/CD

O arquivo `.github/workflows/deploy-prod.yml` executa em sequencia a cada push na `main`:

| Etapa | O que faz |
|-------|-----------|
| `build-test` | Chama `build-test.yml` reutilizavel — `mvn clean verify` com JaCoCo |
| `deploy` | Autentica na AWS, faz login no ECR, build e push da imagem backend, le segredos do Secrets Manager e SSM, aplica manifestos K8s, aguarda DNS do Load Balancer e publica no SSM, opcionalmente faz build e deploy do frontend |

**Observacao sobre autenticacao AWS:** o Learner Lab nao suporta OIDC. O workflow usa credenciais temporarias (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`) que expiram a cada sessao de 4h e precisam ser atualizadas manualmente nos secrets do GitHub.

**GitHub Variables necessarias (Environment `production`):**

| Nome | Descricao |
|------|-----------|
| `AWS_REGION` | Regiao AWS |
| `ECR_REPOSITORY` | Nome do repositorio ECR |
| `EKS_CLUSTER` | Nome do cluster EKS |
| `JWT_SECRET_NAME` | Nome do segredo JWT no Secrets Manager |
| `APP_SECRET_NAME` | Nome do segredo da aplicacao no Secrets Manager (`webhookToken`, `mecanicoPassword`) |

**GitHub Secrets necessarios:**

| Nome | Descricao |
|------|-----------|
| `AWS_ACCESS_KEY_ID` | Chave de acesso temporaria do Learner Lab |
| `AWS_SECRET_ACCESS_KEY` | Chave secreta temporaria do Learner Lab |
| `AWS_SESSION_TOKEN` | Token de sessao temporario do Learner Lab |

### Escalabilidade Automatica (HPA)

- **Minimo:** 2 replicas
- **Maximo:** 4 replicas
- **Gatilho:** CPU media acima de 70%
- **Scale up:** aguarda 60s de estabilizacao
- **Scale down:** aguarda 300s de estabilizacao

Para simular carga:

```bash
kubectl run load-ab \
  --image=alpine \
  --restart=Never \
  -n siase \
  -- /bin/sh -c "apk add apache2-utils -q && ab -n 500000 -c 100 http://app-service.siase.svc.cluster.local:8080/api/actuator/health"

# Monitorar em tempo real
watch -n 5 "kubectl get hpa -n siase && echo '' && kubectl get pods -n siase"

# Parar a carga
kubectl delete pod load-ab -n siase
```

---

## Docker

- **Multi-stage build:** estagio de build com Maven + estagio runtime com JRE
- **Non-root user:** container executa como `appuser`
- **Healthcheck:** `curl` no endpoint `/api/actuator/health`
- **JVM container-aware:** flags `-XX:+UseContainerSupport` e `-XX:MaxRAMPercentage=75.0` respeitam os limites de memoria do pod K8s
- **docker-compose:** healthcheck no PostgreSQL com `depends_on` aguardando `service_healthy`

---

## Testes

```bash
# Rodar todos os testes
./mvnw test

# Rodar com relatorio de cobertura
./mvnw test jacoco:report
```

Os testes estao organizados por modulo seguindo Clean Architecture:

- `siase-domain/src/test/` — testes unitarios de regras de negocio (OrdemDeServico, Peca)
- `siase-application/src/test/` — testes unitarios dos use cases com mocks dos ports
- `siase-infrastructure/src/test/` — testes de controller com MockMvc e integracao com H2

Cobertura minima de 80% de linhas configurada como regra obrigatoria do build via JaCoCo.

---

## Relatorio SonarQube

```bash
# 1. Suba o SonarQube
docker compose -f docker-compose.sonarqube.yml up -d

# 2. Execute o scan Maven
./mvnw sonar:sonar -Dsonar.token=SEU_TOKEN

# 3. Gere o PDF
cd scripts
python sonar_pdf_report.py
```

Pre-requisitos: Python 3.x com `fpdf2`, `matplotlib`, `requests`.

---

## Links

- **Swagger UI (local):** `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api/v3/api-docs`
- **Postman Collection:** `postman/SIASE.postman_collection.json`
- **Link para video demonstrativo:** [A ser adicionado apos gravacao]

---

## Documentacao Arquitetural

- [Diagramas de Sequencia](docs/diagramas-sequencia.md)
- [ADR-001 — Adocao de Arquitetura Hexagonal](docs/adr/ADR-001-arquitetura-hexagonal.md)
- [ADR-002 — Padrao de Comunicacao REST e Autenticacao JWT](docs/adr/ADR-002-rest-jwt.md)
- [RFC-001 — Fluxo de Ordens de Servico e Maquina de Estados](docs/rfc/RFC-001-fluxo-ordens-servico.md)
