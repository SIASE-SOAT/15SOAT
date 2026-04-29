# SIASE - Sistema Integrado de Atendimento e Execução de Serviços

> Pós Tech - Software Architecture | FIAP

## Tecnologias

| Tecnologia         | Versão  | Justificativa                                                                 |
|--------------------|---------|-------------------------------------------------------------------------------|
| Java               | 17      | LTS com suporte estendido, records, sealed classes e melhorias de performance |
| Spring Boot        | 3.2.4   | Framework consolidado, ecossistema maduro, convenção sobre configuração       |
| PostgreSQL         | 16      | Banco relacional robusto, ACID, suporte a JSON, open-source e escalável       |
| Flyway             | —       | Controle de versão do schema de banco via migrations                          |
| Springdoc OpenAPI  | 2.4.0   | Documentação automática da API via anotações                                  |
| Docker / Compose   | —       | Ambiente reproduzível e isolado para dev e produção                           |

### Por que PostgreSQL?

PostgreSQL foi escolhido por ser um banco relacional maduro com suporte a ACID, ideal para sistemas transacionais como atendimentos e execução de serviços. Oferece excelente desempenho, suporte a JSON/JSONB para dados semi-estruturados, extensibilidade e é open-source, reduzindo custos de licença.

## Estrutura do Projeto

```
src/main/java/br/com/fiap/siase/
├── SiaseApplication.java          # Entry point
├── config/                        # Configurações (OpenAPI, Beans)
├── controller/                    # Controllers REST (camada de apresentação)
├── service/                       # Regras de negócio (camada de serviço)
├── repository/                    # Interfaces Spring Data JPA (camada de dados)
├── model/                         # Entidades JPA
├── dto/
│   ├── request/                   # DTOs de entrada
│   └── response/                  # DTOs de saída
└── exception/                     # Exception handler global
```

> A arquitetura atual é MVC em camadas. A migração futura para Clean Architecture / Hexagonal será feita movendo as responsabilidades para `domain/`, `application/` e `infrastructure/`.

## Rodando localmente

### Pré-requisitos

- Docker e Docker Compose instalados
- Java 17+ (para rodar sem Docker)
- Maven 3.9+ (para rodar sem Docker)

---

### 🚀 Opção 1 — Back-end + Banco (avaliação da API)

Sobe apenas o banco de dados e a aplicação Spring Boot. Ideal para avaliar os endpoints via Swagger ou Postman.

```bash
docker compose up --build
```

| Serviço | URL |
|---------|-----|
| API REST | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/api/swagger-ui.html` |

---

### ⭐ Opção 2 — Stack completa: Back-end + Front-end + Banco

Sobe a stack inteira incluindo a interface web Angular. Permite visualizar e operar o sistema completo.

```bash
docker compose -f docker-compose.full.yml up --build
```

| Serviço | URL |
|---------|-----|
| Interface Web | `http://localhost:4200` |
| API REST | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/api/swagger-ui.html` |

---

### 🔧 Opção 3 — Desenvolvimento local (sem Docker para a aplicação)

Útil para desenvolvimento com hot-reload. Sobe somente o banco via Docker e roda a aplicação localmente.

1. Suba o PostgreSQL:
```bash
docker compose up postgres
```

2. Configure as variáveis de ambiente (ou use os valores padrão):
```bash
cp .env.example .env
```

3. Execute a aplicação:
```bash
./mvnw spring-boot:run
```

### Variáveis de Ambiente

| Variável            | Padrão         | Descrição                        |
|---------------------|----------------|----------------------------------|
| `DB_HOST`           | `localhost`    | Host do banco de dados           |
| `DB_PORT`           | `5432`         | Porta do PostgreSQL              |
| `DB_NAME`           | `siase_db`     | Nome do banco                    |
| `DB_USER`           | `siase_user`   | Usuário do banco                 |
| `DB_PASSWORD`       | `siase_pass`   | Senha do banco                   |
| `SERVER_PORT`       | `8080`         | Porta da aplicação               |
| `JWT_SECRET`        | *(ver .env.example)* | Chave HMAC para assinar tokens |
| `JWT_EXPIRATION_MS` | `3600000`      | Expiração do token (ms) — 1h    |

## Primeiros passos (autenticação)

Todos os endpoints administrativos exigem JWT. Após subir a aplicação:

```bash
# 1. Registrar usuário (sem autenticação)
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

A forma recomendada para explorar e testar os endpoints é via **Swagger UI**, disponível assim que a aplicação estiver no ar:

```
http://localhost:8080/api/swagger-ui.html
```

O Swagger documenta todos os endpoints, exibe os schemas de request/response e permite executar chamadas diretamente pelo navegador — basta clicar em **Authorize**, informar o Bearer token obtido no login e chamar qualquer endpoint.

A pasta `postman/` também disponibiliza:

- `SIASE.postman_collection.json` — coleção com todos os endpoints pré-configurados
- `GUIA_DE_TESTES.md` — roteiro com cenários de teste (ciclo completo de OS, cancelamento, controle de estoque, CRUD administrativo)

## Documentação da API

Com a aplicação rodando, acesse:

- **Swagger UI:** `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api/v3/api-docs`

## Relatório SonarQube

O script `scripts/sonar_pdf_report.py` gera um relatório PDF com as métricas do SonarQube. O arquivo gerado é salvo em:

```
scripts/target/sonar/relatorio_sonarqube.pdf
```

### Pré-requisitos

- Python 3.x com as dependências: `fpdf2`, `matplotlib`, `requests`
- SonarQube rodando em `http://localhost:9000` com o projeto `br.com.fiap:siase` já analisado

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

O PDF será criado automaticamente em `scripts/target/sonar/` (o diretório é criado se não existir).

## Testes

```bash
# Rodar todos os testes
./mvnw test

# Rodar com relatório de cobertura
./mvnw test jacoco:report
```
