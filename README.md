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
| Kubernetes (Kind)  | v1.30   | Orquestracao de containers em cluster local na VPS                            |
| Terraform          | 1.7+    | Infraestrutura como codigo — gerencia recursos K8s de forma declarativa       |
| GitHub Actions     | —       | Pipeline CI/CD com build, testes, push de imagem e deploy automatizado        |

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
├── pom.xml                          # POM pai (agregador)
├── siase-domain/                    # Entidades POJO, enums, ports, validacoes
├── siase-application/               # Use cases, DTOs, port interfaces
├── siase-infrastructure/            # JPA entities, mappers MapStruct, controllers, security
├── frontend/                        # Frontend Angular
├── k8s/                             # Manifestos Kubernetes
│   ├── kind-cluster.yaml            # Configuracao do cluster Kind (2 workers + port mapping)
│   ├── namespace.yaml               # Namespace siase
│   ├── configmap.yaml               # Variaveis nao-sensiveis
│   ├── secret.yaml                  # Template de secrets (valores injetados pelo CI)
│   ├── postgres-pvc.yaml            # PersistentVolumeClaim do banco (2 Gi)
│   ├── postgres-deployment.yaml     # Deployment do PostgreSQL 16
│   ├── postgres-service.yaml        # ClusterIP service do banco
│   ├── app-deployment.yaml          # Deployment da aplicacao (2 replicas)
│   ├── app-service.yaml             # NodePort service da aplicacao (porta 30080)
│   └── hpa.yaml                     # HPA: 2-4 replicas, CPU target 70%
├── infra/                           # Infraestrutura como Codigo (Terraform)
│   ├── main.tf                      # Provider kubernetes + todos os recursos K8s
│   ├── variables.tf                 # Variaveis (image_tag, secrets, replicas)
│   ├── outputs.tf                   # Outputs (app_image, nodeport, namespace)
│   └── terraform.tfvars.example     # Exemplo de valores (sem secrets reais)
├── .github/
│   └── workflows/
│       └── ci-cd.yml                # Pipeline CI/CD completo (build → test → docker → deploy)
├── scripts/
│   └── vps-setup.sh                 # Referencia de setup inicial da VPS
├── Dockerfile                       # Multi-stage multi-modulo + JVM container flags
├── docker-compose.yml               # Dev local (backend + DB)
├── docker-compose.full.yml          # Stack completa (+ frontend Angular)
├── docker-compose.sonarqube.yml     # SonarQube para analise estatica
└── postman/                         # Collection e guia de testes
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

---

## Infraestrutura (Fase 2)

### Arquitetura de Deploy

```
┌──────────────────────────────────────────────────────────────┐
│                       GitHub Actions                          │
│                                                              │
│  push → main   ┌─────────────────┐                           │
│  ───────────►  │ build-and-test  │  ubuntu-latest             │
│                │ mvn clean verify│                            │
│                └────────┬────────┘                            │
│                         │ ok                                  │
│                ┌────────▼────────┐                            │
│                │docker-build-push│  ubuntu-latest             │
│                │ push → ghcr.io  │                            │
│                └────────┬────────┘                            │
│                         │ image_tag (git SHA)                 │
│                ┌────────▼──────────────────┐                  │
│                │        deploy             │  ubuntu-latest   │
│                │ 1. Conecta Tailscale VPN  │                  │
│                │ 2. SSH via IP privado VPN │                  │
│                │ 3. terraform apply        │                  │
│                │ 4. kubectl rollout status │                  │
│                │ 5. curl /health           │                  │
│                └───────────────┬───────────┘                  │
└───────────────────────────────────────────────────────────────┘
                                 │ Tailscale VPN (tunel privado)
┌────────────────────────────────▼──────────────────────────────┐
│              VPS Hostinger — Ubuntu 24.04 · 8 GB · 2 vCPU     │
│                                                               │
│   ┌───────────────────────────────────────────────────────┐   │
│   │              Kind Cluster (K8s v1.30)                  │   │
│   │   1 control-plane + 2 workers · namespace: siase       │   │
│   │                                                        │   │
│   │   ┌──────────────┐    ┌─────────────────────────────┐ │   │
│   │   │   postgres   │    │         siase-app           │ │   │
│   │   │  1 replica   │◄───│   2-4 replicas (HPA CPU 70%)│ │   │
│   │   │  PVC 2Gi     │    │   imagePullSecret: ghcr.io  │ │   │
│   │   └──────────────┘    └─────────────────────────────┘ │   │
│   │                                                        │   │
│   │   metrics-server · NodePort 30080 → VPS:8080          │   │
│   └───────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────┘
```

### Deploy em Kubernetes

**Pre-requisitos na VPS:** cluster Kind rodando (ver `scripts/vps-setup.sh`) e Tailscale instalado e conectado.

O deploy e feito automaticamente pelo CI/CD a cada push na `main`. Para aplicar manualmente:

```bash
# 1. Criar o cluster (primeira vez)
kind create cluster --name siase --config k8s/kind-cluster.yaml

# 2. Aplicar o namespace
kubectl apply -f k8s/namespace.yaml

# 3. Provisionar via Terraform
cd infra
cp terraform.tfvars.example terraform.tfvars
# Editar terraform.tfvars com os valores reais

terraform init
terraform apply
```

Verificar o deploy:

```bash
kubectl get pods -n siase
kubectl get hpa -n siase
curl http://<IP_DA_VPS>:8080/api/actuator/health
```

### Provisionamento com Terraform

O Terraform gerencia todos os recursos K8s declarativamente: ConfigMap, Secret, PVC, Deployments (postgres e app), Services e HPA.

```bash
cd infra

# Inicializar providers
terraform init

# Ver o plano sem aplicar
terraform plan \
  -var="image_tag=abc1234" \
  -var="db_password=senha" \
  -var="jwt_secret=segredo" \
  -var="webhook_token=token" \
  -var="mecanico_password=senha"

# Aplicar
terraform apply -auto-approve
```

**Recursos criados pelo Terraform:**

| Recurso | Tipo | Descricao |
|---------|------|-----------|
| `siase-config` | ConfigMap | Variaveis nao-sensiveis do app |
| `siase-secret` | Secret | Credenciais do banco e tokens |
| `postgres-pvc` | PVC | Volume persistente do banco (2 Gi) |
| `postgres` | Deployment | PostgreSQL 16-alpine |
| `postgres-service` | Service | ClusterIP interno para o banco |
| `siase-app` | Deployment | Aplicacao Spring Boot (2 replicas) |
| `app-service` | Service | NodePort 30080 → porta 8080 |
| `siase-app-hpa` | HPA | Escala de 2 a 4 replicas por CPU |

### Pipeline CI/CD

O arquivo `.github/workflows/ci-cd.yml` executa 3 jobs em sequencia a cada push na `main`:

| Job | Runner | O que faz |
|-----|--------|-----------|
| `build-and-test` | ubuntu-latest | `mvn clean verify` — compila e roda todos os testes |
| `docker-build-push` | ubuntu-latest | Build da imagem e push para `ghcr.io` com tag = git SHA usando `GITHUB_TOKEN` nativo |
| `deploy` | ubuntu-latest | Conecta via Tailscale VPN, SSH na VPS, `terraform apply`, `kubectl rollout status`, smoke test |

**GitHub Secrets necessarios:**

| Secret | Descricao |
|--------|-----------|
| `MY_PAT` | Personal Access Token com `repo` + `read:packages` + `write:packages` — usado para git pull e imagePullSecret |
| `TAILSCALE_AUTHKEY` | Auth key reutilizavel do Tailscale para o runner entrar na VPN |
| `VPS_TAILSCALE_IP` | IP privado da VPS na rede Tailscale |
| `VPS_PASSWORD` | Senha root da VPS para o SSH |
| `DB_PASSWORD` | Senha do PostgreSQL |
| `JWT_SECRET` | Chave HMAC para tokens JWT |
| `WEBHOOK_TOKEN` | Token de autenticacao de webhooks |
| `MECANICO_PASSWORD` | Senha do usuario padrao seed |

### Escalabilidade Automatica (HPA)

O HPA monitora o consumo de CPU dos pods da aplicacao e escala automaticamente:

- **Minimo:** 2 replicas
- **Maximo:** 4 replicas
- **Gatilho:** CPU media acima de 70%
- **Scale up:** aguarda 60s de estabilizacao
- **Scale down:** aguarda 300s de estabilizacao

Para simular carga e observar o escalonamento:

```bash
# Gerar carga
kubectl run load-generator --image=busybox:1.35 --restart=Never -- \
  /bin/sh -c "while true; do wget -q -O- http://app-service.siase.svc.cluster.local:8080/api/actuator/health; done"

# Acompanhar replicas em tempo real
kubectl get hpa -n siase -w

# Parar a carga
kubectl delete pod load-generator
```

---

## Docker

- **Multi-stage build:** estagio de build com Maven + estagio runtime com JRE
- **Non-root user:** container executa como `appuser`
- **Healthcheck:** `curl` no endpoint `/api/actuator/health` com Spring Boot Actuator
- **JVM container-aware:** flags `-XX:+UseContainerSupport` e `-XX:MaxRAMPercentage=75.0` respeitam os limites de memoria do pod K8s
- **docker-compose:** healthcheck no PostgreSQL com `depends_on` aguardando `service_healthy`

## Links

- **Swagger UI (local):** `http://localhost:8080/api/swagger-ui.html`
- **Swagger UI (producao):** `http://<IP_DA_VPS>:8080/api/swagger-ui.html`
- **Postman Collection:** `postman/SIASE.postman_collection.json`
- **Link para video demonstrativo:** [A ser adicionado apos gravacao]
