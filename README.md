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

### Com Docker Compose (recomendado)

```bash
# Subir toda a stack (banco + aplicação)
docker compose up --build

# Somente o banco (para desenvolvimento local)
docker compose up postgres
```

A API estará disponível em: `http://localhost:8080/api`

### Sem Docker (desenvolvimento)

1. Suba o PostgreSQL localmente ou via Docker:
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

| Variável      | Padrão       | Descrição                  |
|---------------|--------------|----------------------------|
| `DB_HOST`     | `localhost`  | Host do banco de dados      |
| `DB_PORT`     | `5432`       | Porta do PostgreSQL         |
| `DB_NAME`     | `siase_db`   | Nome do banco               |
| `DB_USER`     | `siase_user` | Usuário do banco            |
| `DB_PASSWORD` | `siase_pass` | Senha do banco              |
| `SERVER_PORT` | `8080`       | Porta da aplicação          |

## Documentação da API

Com a aplicação rodando, acesse:

- **Swagger UI:** `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api/v3/api-docs`

## Testes

```bash
# Rodar todos os testes
./mvnw test

# Rodar com relatório de cobertura
./mvnw test jacoco:report
```
