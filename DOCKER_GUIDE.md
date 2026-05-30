# Guia de Execução - SIASE Com Docker

## Opção 1: Rodar Tudo no Docker (Backend + Frontend)

### Pré-requisitos
- Docker instalado
- Docker Compose instalado

### Executar com Frontend
```bash
# Na raiz do projeto - use o arquivo full
docker-compose -f docker-compose.full.yml up --build

# A aplicação estará disponível em:
# Frontend: http://localhost:4200
# Backend: http://localhost:8080/api
# Swagger: http://localhost:8080/api/swagger-ui.html
```

### Parar
```bash
docker-compose -f docker-compose.full.yml down
```

---

## Opção 2: Rodar Apenas Backend + Banco (Padrão)

### Executar
```bash
# Na raiz do projeto - arquivo padrão (recomendado para desenvolvimento)
docker-compose up --build

# A aplicação estará disponível em:
# Backend: http://localhost:8080/api
# Database: localhost:5432
```

### Parar
```bash
docker-compose down
```

---

## Opção 3: Rodar Localmente (Desenvolvimento)

### Backend

#### Pré-requisitos
- Java 17+
- Maven 3.9+
- PostgreSQL rodando (pode ser em Docker)

#### Executar PostgreSQL
```bash
docker run -d \
  --name siase-postgres \
  -e POSTGRES_DB=siase_db \
  -e POSTGRES_USER=siase_user \
  -e POSTGRES_PASSWORD=siase_pass \
  -p 5432:5432 \
  postgres:16-alpine
```

#### Executar Backend
```bash
cd /d/Projetos/15SOAT

# Instalar modulos no repositorio local
./mvnw install -DskipTests

# Executar a aplicacao
./mvnw spring-boot:run -pl siase-infrastructure
# ou se estiver no Windows
mvnw.cmd spring-boot:run -pl siase-infrastructure
```

Backend disponível em: `http://localhost:8080/api`

### Frontend

#### Pré-requisitos
- Node.js 20+
- npm 10+

#### Executar
```bash
cd d:\Projetos\15SOAT\frontend
npm install
npm start
```

Frontend disponível em: `http://localhost:4200`

---

## Problemas Comuns

### CORS Error no Docker
**Problema:** `Access to XMLHttpRequest at 'http://localhost:8080/api/...' blocked by CORS`

**Solução:** Já está configurado! O nginx faz proxy reverso e adiciona headers CORS.

### Porta já em uso
```bash
# Para liberar a porta (exemplo porta 4200)
# Windows
netstat -ano | findstr :4200
taskkill /PID <PID> /F

# Mac/Linux
lsof -i :4200
kill -9 <PID>
```

### Build falha no Docker
```bash
# Limpar cache do Docker
docker-compose down -v
docker system prune -a
docker-compose up --build
```

---

## Estrutura dos Arquivos Docker Compose

### `docker-compose.yml` (Padrão)
Para desenvolvimento com Backend + Database apenas

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| **siase-postgres** | 5432 | Banco de dados PostgreSQL |
| **siase-app** | 8080 | Backend Java Spring Boot |

### `docker-compose.full.yml` (Completo)
Para rodar tudo - Backend + Frontend + Database

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| **siase-postgres** | 5432 | Banco de dados PostgreSQL |
| **siase-app** | 8080 | Backend Java Spring Boot |
| **siase-frontend** | 4200 | Frontend Angular com Nginx |

---

## Variáveis de Ambiente

Ver `.env.example` para mais detalhes. Você pode criar um `.env` local baseado nele.
