# Passo a passo completo para produção

---

## PASSO 1 — S3 bucket para Terraform state

**Onde:** AWS Console ou CLI  
**Por quê:** os 3 repositórios de infra (`siase-infra-k8s`, `siase-infra-database`, `siase-auth-lambda`) guardam o estado do Terraform nesse bucket. Sem ele o `terraform init` falha.

```bash
aws s3api create-bucket --bucket siase-tfstate-thiago-15soat --region us-east-1
aws s3api put-bucket-versioning --bucket siase-tfstate-thiago-15soat --versioning-configuration Status=Enabled
```

---

## PASSO 2 — ECR repositories

**Onde:** AWS Console ou CLI  
**Por quê:** os workflows fazem `docker push` para o ECR antes de subir no EKS. O repositório precisa existir antes do primeiro push.

```bash
aws ecr create-repository --repository-name siase-app
aws ecr create-repository --repository-name siase-os-service
aws ecr create-repository --repository-name siase-billing-service
aws ecr create-repository --repository-name siase-execution-service
```

---

## PASSO 3 — Secrets no AWS Secrets Manager

**Onde:** AWS Console ou CLI  
**Por quê:** os workflows buscam esses secrets em tempo de deploy para injetar nas aplicações. Precisam existir antes dos deploys.

```bash
# JWT — usado por todos os serviços para validar tokens
aws secretsmanager create-secret \
  --name "siase/production/jwt" \
  --secret-string '{"secret":"<string-longa-e-aleatoria>"}'

# App secrets — usado pelo monolito 15SOAT
aws secretsmanager create-secret \
  --name "siase/production/app-secrets" \
  --secret-string '{"webhookToken":"<token-webhook>","mecanicoPassword":"<senha-mecanico>"}'

# Grafana — usado pelo siase-infra-k8s para configurar o Grafana
aws secretsmanager create-secret \
  --name "siase/production/grafana" \
  --secret-string '{"password":"<senha-grafana>"}'

# Mercado Pago — usado pelo siase-billing-service
aws secretsmanager create-secret \
  --name "siase/production/mercadopago" \
  --secret-string '{"accessToken":"<seu-token-mercadopago>"}'
```

Após criar, anote o ARN do grafana secret:

```bash
aws secretsmanager describe-secret --secret-id "siase/production/grafana" --query 'ARN' --output text
```

---

## PASSO 4 — SQS Queues

**Onde:** AWS Console ou CLI  
**Por quê:** são as filas de mensagens entre os microserviços. Os serviços tentam publicar e consumir mensagens assim que sobem — se a fila não existir, falham. Após criar, anote as URLs.

```bash
aws sqs create-queue --queue-name siase-billing-queue
aws sqs create-queue --queue-name siase-os-eventos
aws sqs create-queue --queue-name siase-os-updates
aws sqs create-queue --queue-name siase-execution-queue
```

Para pegar as URLs depois:

```bash
aws sqs get-queue-url --queue-name siase-billing-queue --query 'QueueUrl' --output text
aws sqs get-queue-url --queue-name siase-os-eventos --query 'QueueUrl' --output text
aws sqs get-queue-url --queue-name siase-os-updates --query 'QueueUrl' --output text
aws sqs get-queue-url --queue-name siase-execution-queue --query 'QueueUrl' --output text
```

---

## PASSO 5 — DynamoDB Tables

**Onde:** AWS Console ou CLI  
**Por quê:** diferente do RDS, o DynamoDB não tem migração automática. As tabelas precisam existir antes dos serviços subirem.

```bash
# siase-execution-fila — fila de trabalho dos mecânicos, chave primária é osId
aws dynamodb create-table \
  --table-name siase-execution-fila \
  --attribute-definitions AttributeName=osId,AttributeType=S \
  --key-schema AttributeName=osId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

# siase-os-eventos — histórico de mudanças de status das OS
# precisa de GSI porque o código faz query por osId mas a PK é id
aws dynamodb create-table \
  --table-name siase-os-eventos \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
    AttributeName=osId,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --global-secondary-indexes '[{
    "IndexName": "osId-index",
    "KeySchema": [{"AttributeName":"osId","KeyType":"HASH"}],
    "Projection": {"ProjectionType":"ALL"}
  }]' \
  --billing-mode PAY_PER_REQUEST
```

---

## PASSO 6 — GitHub: configurar Secrets e Variables

**Onde:** em cada repositório → Settings → Secrets and variables → Actions  
**Por quê:** os workflows leem credenciais AWS dos Secrets e configurações das Variables.

> Os **Secrets** precisam ser atualizados a cada sessão do Learner Lab (~4h). As **Variables** são configuradas uma única vez.

### Secrets — em TODOS os repositórios

| Secret | Valor | Por quê |
|---|---|---|
| `AWS_ACCESS_KEY_ID` | AWS Academy → AWS Details | Autenticar na AWS |
| `AWS_SECRET_ACCESS_KEY` | AWS Academy → AWS Details | Autenticar na AWS |
| `AWS_SESSION_TOKEN` | AWS Academy → AWS Details | Autenticar na AWS |

---

### Variables — `siase-infra-k8s`

| Variable | Valor | Por quê |
|---|---|---|
| `AWS_REGION` | `us-east-1` | Região onde tudo será criado |
| `TF_STATE_BUCKET` | `siase-tfstate-thiago-15soat` | Bucket do Terraform state |
| `LAB_ROLE_ARN` | `arn:aws:iam::<account-id>:role/LabRole` | Role que o Terraform usa para criar recursos |
| `GRAFANA_SECRET_ARN` | ARN anotado no Passo 3 | Terraform lê a senha do Grafana desse secret |
| `EKS_CLUSTER_NAME` | `siase-production` | Nome do cluster EKS que será criado |
| `LB_DNS_SSM_PARAMETER` | `/siase/production/lb-dns` | Parâmetro SSM onde o DNS do Load Balancer será salvo |

---

### Variables — `siase-infra-database`

| Variable | Valor | Por quê |
|---|---|---|
| `AWS_REGION` | `us-east-1` | Região |
| `TF_STATE_BUCKET` | `siase-tfstate-thiago-15soat` | Bucket do Terraform state |

---

### Variables — `siase-auth-lambda`

| Variable | Valor | Por quê |
|---|---|---|
| `AWS_REGION` | `us-east-1` | Região |
| `TF_STATE_BUCKET` | `siase-tfstate-thiago-15soat` | Bucket do Terraform state |
| `LAB_ROLE_ARN` | `arn:aws:iam::<account-id>:role/LabRole` | Role do Terraform |
| `VPC_ID` | ID da VPC criada pelo `siase-infra-k8s` | Lambda precisa estar na mesma VPC do RDS |
| `PRIVATE_SUBNET_IDS` | `subnet-xxx,subnet-yyy` | Subnets privadas da VPC |
| `LAMBDA_SECURITY_GROUP_ID` | SG do node group do EKS | Permite Lambda acessar o RDS |
| `JWT_SECRET_NAME` | `siase/production/jwt` | Nome do secret JWT no Secrets Manager |

> `VPC_ID`, `PRIVATE_SUBNET_IDS` e `LAMBDA_SECURITY_GROUP_ID` ficam disponíveis nos outputs do Terraform do `siase-infra-k8s` após o Passo 7.

---

### Variables — `15SOAT`

| Variable | Valor | Por quê |
|---|---|---|
| `AWS_REGION` | `us-east-1` | Região |
| `ECR_REPOSITORY` | `siase-app` | Repositório ECR criado no Passo 2 |
| `EKS_CLUSTER` | `siase-production` | Cluster onde o deploy acontece |
| `JWT_SECRET_NAME` | `siase/production/jwt` | Workflow busca o JWT secret pelo nome |
| `APP_SECRET_NAME` | `siase/production/app-secrets` | Workflow busca webhook token e senha do mecânico |

---

### Variables — `siase-os-service`

| Variable | Valor | Por quê |
|---|---|---|
| `AWS_REGION` | `us-east-1` | Região |
| `EKS_CLUSTER` | `siase-production` | Cluster do deploy |
| `ECR_REPOSITORY_OS` | `siase-os-service` | Repositório ECR |
| `OS_DB_HOST` | endpoint do RDS | Disponível após Passo 7 via SSM |
| `OS_DB_USER` | `siase_master` | Usuário master criado pelo Terraform do RDS |
| `OS_DB_SECRET` | ARN do secret do RDS | Disponível após Passo 7 via SSM |
| `JWT_SECRET_NAME` | `siase/production/jwt` | Nome do secret JWT |
| `SQS_OS_EVENTOS_URL` | URL da fila `siase-os-eventos` | Criada no Passo 4 |
| `SQS_BILLING_URL` | URL da fila `siase-billing-queue` | Criada no Passo 4 |
| `SQS_EXECUTION_URL` | URL da fila `siase-execution-queue` | Criada no Passo 4 |
| `SQS_OS_UPDATES_URL` | URL da fila `siase-os-updates` | Criada no Passo 4 |

---

### Variables — `siase-billing-service`

| Variable | Valor | Por quê |
|---|---|---|
| `AWS_REGION` | `us-east-1` | Região |
| `EKS_CLUSTER` | `siase-production` | Cluster do deploy |
| `ECR_REPOSITORY_BILLING` | `siase-billing-service` | Repositório ECR |
| `BILLING_DB_HOST` | endpoint do RDS | Disponível após Passo 7 via SSM |
| `BILLING_DB_USER` | `siase_master` | Usuário master do RDS |
| `BILLING_DB_SECRET` | ARN do secret do RDS | Disponível após Passo 7 via SSM |
| `JWT_SECRET_NAME` | `siase/production/jwt` | Nome do secret JWT |
| `MP_SECRET_NAME` | `siase/production/mercadopago` | Nome do secret do Mercado Pago |
| `SQS_BILLING_URL` | URL da fila `siase-billing-queue` | Criada no Passo 4 |
| `SQS_OS_UPDATES_URL` | URL da fila `siase-os-updates` | Criada no Passo 4 |
| `SQS_EXECUTION_URL` | URL da fila `siase-execution-queue` | Criada no Passo 4 |
| `BILLING_PUBLIC_URL` | URL pública do API Gateway | Disponível após Passo 9 |
| `PORTAL_URL` | URL pública do frontend | Disponível após Passo 10 |

---

### Variables — `siase-execution-service`

| Variable | Valor | Por quê |
|---|---|---|
| `AWS_REGION` | `us-east-1` | Região |
| `EKS_CLUSTER` | `siase-production` | Cluster do deploy |
| `ECR_REPOSITORY_EXECUTION` | `siase-execution-service` | Repositório ECR |
| `EXEC_DB_HOST` | endpoint do RDS | Disponível após Passo 7 via SSM |
| `EXEC_DB_USER` | `siase_master` | Usuário master do RDS |
| `EXEC_DB_SECRET` | ARN do secret do RDS | Disponível após Passo 7 via SSM |
| `JWT_SECRET_NAME` | `siase/production/jwt` | Nome do secret JWT |
| `SQS_EXECUTION_URL` | URL da fila `siase-execution-queue` | Criada no Passo 4 |
| `SQS_OS_UPDATES_URL` | URL da fila `siase-os-updates` | Criada no Passo 4 |

---

## PASSO 7 — Deploy da infraestrutura (ordem obrigatória)

**7a. Push na main do `siase-infra-k8s`** → cria VPC, subnets, EKS cluster, node group, Prometheus, Grafana, Loki. Após concluir, pegue os valores para as variables do Passo 6:

```bash
aws ssm get-parameter --name "/siase/production/vpc-id" --query 'Parameter.Value' --output text
aws ssm get-parameter --name "/siase/production/private-subnet-ids" --query 'Parameter.Value' --output text
aws ssm get-parameter --name "/siase/production/eks-node-sg-id" --query 'Parameter.Value' --output text
```

Atualize as variables `VPC_ID`, `PRIVATE_SUBNET_IDS` e `LAMBDA_SECURITY_GROUP_ID` no `siase-auth-lambda`.

**7b. Push na main do `siase-infra-database`** → cria a instância RDS PostgreSQL. Após concluir, pegue os valores:

```bash
aws ssm get-parameter --name "/siase/production/db-endpoint" --query 'Parameter.Value' --output text
aws ssm get-parameter --name "/siase/production/db-secret-arn" --query 'Parameter.Value' --output text
```

Atualize `OS_DB_HOST`, `BILLING_DB_HOST`, `EXEC_DB_HOST` e os ARNs dos secrets nos repos dos microserviços.

---

## PASSO 8 — Criar os bancos de dados dentro do RDS

**Onde:** conectando no RDS após ele estar de pé  
**Por quê:** o Terraform cria a instância com o banco `siase`, mas os microserviços precisam de bancos separados. O Flyway cria as tabelas automaticamente, mas o banco precisa existir antes.

```bash
ENDPOINT=$(aws ssm get-parameter --name "/siase/production/db-endpoint" --query 'Parameter.Value' --output text)

psql -h $ENDPOINT -U siase_master -d siase -c "CREATE DATABASE siase_os_db;"
psql -h $ENDPOINT -U siase_master -d siase -c "CREATE DATABASE siase_billing_db;"
psql -h $ENDPOINT -U siase_master -d siase -c "CREATE DATABASE siase_exec_db;"
```

---

## PASSO 9 — Push na main do `siase-auth-lambda`

Cria a Lambda de autenticação e o API Gateway. Após concluir, o endpoint público fica disponível no SSM:

```bash
aws ssm get-parameter --name "/siase/production/api-endpoint" --query 'Parameter.Value' --output text
```

Atualize a variable `BILLING_PUBLIC_URL` no `siase-billing-service` com esse valor.

---

## PASSO 10 — Deploy das aplicações

Push na main de cada repositório — podem ser feitos em paralelo:

- `siase-os-service`
- `siase-billing-service`
- `siase-execution-service`
- `15SOAT` (monolito + frontend)

Após o `15SOAT` concluir, o frontend estará disponível. Atualize `PORTAL_URL` no `siase-billing-service` e faça um novo push para atualizar o configmap com a URL correta.
