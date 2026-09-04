# Roteiro — Vídeo Tech Challenge Fase 3
# SIASE — Segurança, Observabilidade e Operação Corporativa

> Tempo estimado: 8 a 12 minutos
> Ferramenta de gravação sugerida: OBS Studio ou Loom
> Resolução recomendada: 1920x1080

---

## ANTES DE GRAVAR — Checklist de preparação

**AWS Learner Lab**
- [ ] Sessão do Learner Lab iniciada e créditos verificados
- [ ] Credenciais temporárias atualizadas nos secrets do GitHub (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`)
- [ ] Cluster EKS ativo: `aws eks update-kubeconfig --region us-east-1 --name siase-production`
- [ ] Pods rodando: `kubectl get pods -n siase` — todos em `Running`
- [ ] DNS do Load Balancer anotado: `kubectl get svc -n siase app-service` — coluna `EXTERNAL-IP`
- [ ] URL base da API anotada: `http://<EXTERNAL-IP>/api`
- [ ] URL do API Gateway anotada (para fluxo de cliente via Lambda)

**Ferramentas locais**
- [ ] Postman com a collection `SIASE.postman_collection.json` importada e variável `baseUrl` apontando para `http://<EXTERNAL-IP>/api`
- [ ] Swagger aberto: `http://<EXTERNAL-IP>/api/swagger-ui.html`
- [ ] Terminal aberto com kubeconfig configurado para o cluster EKS
- [ ] GitHub Actions com último workflow `deploy-prod.yml` executado com sucesso
- [ ] Grafana aberto via port-forward ou LoadBalancer do namespace `monitoring`
- [ ] README.md aberto no editor para referência visual da arquitetura
- [ ] Navegador com abas prontas: Swagger, GitHub Actions, Console AWS (EKS + API Gateway + CloudWatch), Grafana

---

## PARTE 1 — Apresentação do Projeto (1 min)

**[Câmera ou voz sobre o README.md aberto no editor]**

> "Olá! Vou apresentar o SIASE — Sistema Integrado de Atendimento e Execução de Serviços,
> desenvolvido para o Tech Challenge Fase 3 da Pós Tech de Arquitetura de Software da FIAP.
>
> O SIASE é um sistema de gestão de oficina mecânica que evoluiu ao longo de 3 fases:
> - Fase 1: MVP com DDD, APIs REST e autenticação JWT
> - Fase 2: Refatoração para Arquitetura Hexagonal, Kubernetes e CI/CD
> - Fase 3 — que é o foco deste vídeo: API Gateway com Lambda Authorizer,
>   observabilidade completa com Prometheus, Grafana e Loki,
>   EKS gerenciado na AWS e estrutura de 4 repositórios independentes."

**[Mostrar o diagrama de arquitetura no README — seção "Visão Geral — Fase 3"]**

> "A arquitetura da Fase 3 tem o seguinte fluxo:
> toda requisição entra pelo AWS API Gateway.
> Para autenticação de clientes, existe um Lambda que valida o CPF, consulta o RDS e emite um JWT.
> Para rotas protegidas, um Lambda Authorizer verifica o JWT antes de encaminhar ao EKS.
> No cluster EKS temos a aplicação com 2 a 4 réplicas gerenciadas pelo HPA,
> e toda a stack de observabilidade: Prometheus, Grafana, Alertmanager, Loki e Grafana Alloy."

---

## PARTE 2 — Estrutura de Repositórios e Arquitetura Hexagonal (1,5 min)

**[Mostrar a estrutura de pastas no terminal ou no editor]**

> "O projeto está organizado em 4 repositórios independentes, cada um com CI/CD próprio:
> - 15SOAT: aplicação principal Spring Boot + Angular + manifestos K8s
> - siase-auth-lambda: Lambda de autenticação e API Gateway
> - siase-infra-k8s: EKS, VPC e stack de observabilidade
> - siase-infra-database: RDS PostgreSQL com KMS e Secrets Manager"

**[Mostrar o diagrama de Clean Architecture no README]**

> "A aplicação segue Arquitetura Hexagonal com 3 módulos Maven independentes.
> A regra de dependência é: infrastructure → application → domain.
> O compilador impede violações de arquitetura em tempo de build."

**[Abrir o explorador de arquivos mostrando siase-domain, siase-application, siase-infrastructure]**

> "No módulo domain temos as entidades POJO, enums como StatusOS, e as interfaces de porta.
> No módulo application temos os 13 use cases — CriarOrdemServicoUC, AprovarOrcamentoUC,
> AvancarStatusUC, entre outros — e os DTOs de entrada e saída.
> No módulo infrastructure ficam os adaptadores: controllers REST, JPA, segurança JWT,
> e o adaptador de observabilidade com Micrometer."

---

## PARTE 3 — Segurança: JWT + API Gateway + Lambda Authorizer (2 min)

**[Abrir o Swagger UI: http://<EXTERNAL-IP>/api/swagger-ui.html — rodando no EKS]**

> "Vou demonstrar o fluxo de segurança com a aplicação rodando no EKS na AWS.
> A aplicação tem dois tipos de autenticação."

**[No Postman, executar POST /api/auth/registrar apontando para o Load Balancer do EKS]**

```json
{ "username": "atendente1", "password": "Atend@2024" }
```

> "Primeiro, o registro de usuário administrativo — mecânicos e atendentes.
> Essa requisição está indo direto para o Load Balancer do EKS na AWS."

**[Executar POST /api/auth/login]**

```json
{ "username": "atendente1", "password": "Atend@2024" }
```

> "O login retorna um JWT assinado com HMAC HS256. O segredo JWT está armazenado
> no AWS Secrets Manager e é injetado no pod via Kubernetes Secret criado pelo CI/CD.
> Senhas são armazenadas com hash BCrypt."

**[Abrir o Console AWS — API Gateway — mostrar a HTTP API criada]**

> "Para autenticação de clientes, o fluxo passa pelo AWS API Gateway.
> Aqui vemos a HTTP API com duas rotas:
> - POST /auth/token — pública, aciona o Lambda Token
> - ANY /{proxy+} — protegida, passa pelo Lambda Authorizer antes de chegar ao EKS."

**[No Postman, executar POST <URL_API_GATEWAY>/auth/token]**

```json
{ "cpf": "529.982.247-25" }
```

> "O Lambda Token valida o CPF, consulta o RDS PostgreSQL e emite um JWT para o cliente.
> Esse token é diferente do JWT administrativo — ele identifica o cliente pelo CPF."

**[Executar uma rota protegida via API Gateway com o token de cliente no header Authorization]**

> "Com o token do cliente, a requisição passa pelo Lambda Authorizer.
> Ele verifica a assinatura HS256, o issuer, a expiração e o clienteId.
> Se válido, encaminha para o EKS com o contexto JWT. Se inválido, retorna 403 aqui mesmo,
> sem nem chegar na aplicação."

**[Mostrar o diagrama de sequência da Fase 3 no docs/diagramas-sequencia.md — item 5]**

> "Esse fluxo desacopla completamente a autenticação de clientes da aplicação principal.
> As rotas públicas — acompanhamento de OS e aprovação de orçamento — continuam
> sem exigir token, acessíveis diretamente pelo Load Balancer do EKS."

---

## PARTE 4 — Demonstração do Fluxo Principal de OS (2,5 min)

**[Usar o Postman com a collection SIASE — pasta "Fluxo Principal" — variável `baseUrl` apontando para o Load Balancer do EKS]**

> "Vou executar o fluxo completo de uma Ordem de Serviço com a aplicação rodando no EKS.
> Todas as requisições estão indo para o Load Balancer provisionado na AWS."

**Passo a passo a executar e narrar:**

1. **Login** — `POST http://<EXTERNAL-IP>/api/auth/login` → token salvo automaticamente
   > "Token JWT obtido. O segredo de assinatura veio do Secrets Manager via CI/CD."

2. **Preparar abertura** — `GET /api/ordens/preparar-abertura?documento=52998224725&placa=ABC1234`
   > "Identificamos o cliente pelo CPF e validamos que a placa pertence a ele.
   > O banco de dados é o RDS PostgreSQL em subnet privada — a aplicação acessa via endpoint interno."

3. **Criar OS** — `POST /api/ordens` com clienteId, veiculoId, serviços e peças
   > "A OS é criada no status RECEBIDA. O orçamento é calculado automaticamente.
   > O estoque das peças é deduzido imediatamente."

4. **Avançar para EM_DIAGNOSTICO** — `PATCH /api/ordens/{id}/avancar`

5. **Avançar para AGUARDANDO_APROVACAO** — `PATCH /api/ordens/{id}/avancar`
   > "E-mail disparado ao cliente. Nos logs do pod — que veremos no Grafana/Loki —
   > aparece o prefixo [EMAIL] com os detalhes."

6. **Cliente aprova** — `PATCH /api/ordens/acompanhar/{numero}/aprovar-orcamento`
   > "Endpoint público — sem token. Acessível diretamente pelo Load Balancer.
   > Status muda para APROVADO."

7. **Avançar para EM_EXECUCAO** — `PATCH /api/ordens/{id}/avancar`

8. **Iniciar e finalizar item** — `PATCH /api/ordens/{id}/itens-servico/{itemId}/iniciar` e `/finalizar`
   > "Registramos os timestamps de início e fim. Esses dados alimentam
   > a métrica siase_execucao_item_tempo_seconds que já está sendo coletada pelo Prometheus no cluster."

9. **Avançar para FINALIZADA** — `PATCH /api/ordens/{id}/avancar`

10. **Registrar pagamento** — `POST /api/ordens/{id}/pagamento` com `{ "formaPagamento": "PIX", "valor": 165.90 }`

11. **Confirmar pagamento** — `PATCH /api/pagamentos/{id}/confirmar`
    > "OS avança automaticamente para ENTREGUE."

**[Mostrar o diagrama de fluxo de status no README]**

> "O fluxo completo é: RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → APROVADO
> → EM_EXECUCAO → FINALIZADA → ENTREGUE. O cancelamento é permitido até APROVADO."

---

## PARTE 5 — Observabilidade: Métricas, Logs e Dashboards (2 min)

**[No terminal, fazer port-forward do serviço de management para confirmar as métricas]**

```bash
kubectl port-forward svc/app-metrics 8081:8081 -n siase
```

> "A porta 8081 é o servidor de management — separada da porta 8080 da aplicação.
> Ela não é exposta pelo Load Balancer público, apenas internamente no cluster."

**[Abrir http://localhost:8081/actuator/prometheus no browser]**

> "Aqui estão as métricas customizadas sendo coletadas pelo Prometheus no EKS:
> - siase_ordens_servico_criadas_total: contador de OS criadas
> - siase_ordem_servico_tempo_status_seconds: histograma de tempo por status
> - siase_execucao_item_iniciadas_total: itens de serviço iniciados
> - siase_execucao_item_tempo_seconds: tempo de execução de cada item
> - siase_falhas_integracao_total: falhas em integrações externas"

**[Mostrar o arquivo k8s/servicemonitor.yaml no editor]**

> "O ServiceMonitor instrui o Prometheus — que está no namespace monitoring do mesmo cluster —
> a coletar esse endpoint a cada 30 segundos."

**[Abrir o Grafana — via port-forward ou LoadBalancer do namespace monitoring]**

```bash
kubectl port-forward svc/kube-prometheus-stack-grafana 3000:80 -n monitoring
```

> "No Grafana, as métricas do SIASE aparecem em tempo real.
> Após o fluxo de OS que executamos, já podemos ver o contador
> siase_ordens_servico_criadas_total incrementado."

**[No Grafana, abrir o Explore e consultar o Loki]**

> "Para os logs, o Grafana Alloy coleta os logs dos pods e envia ao Loki.
> Todos os logs são JSON com os campos correlationId e subject.
> Aqui consigo filtrar todos os logs de uma requisição específica pelo correlationId —
> isso é fundamental para rastrear uma OS do início ao fim em produção."

**[Mostrar o endpoint de health no terminal]**

```bash
kubectl get pods -n siase
```

> "Os pods estão Running porque as probes de readiness e liveness estão passando.
> Readiness em /actuator/health/readiness e liveness em /actuator/health/liveness,
> ambas na porta 8081 — o Kubernetes verifica automaticamente."

---

## PARTE 6 — Kubernetes, HPA e CI/CD (1,5 min)

**[No terminal, mostrar o estado real do cluster EKS]**

```bash
kubectl get pods -n siase
kubectl get hpa -n siase
kubectl get svc -n siase
```

> "Aqui vemos o cluster EKS rodando na AWS. Temos 2 pods do siase-app em Running —
> o mínimo configurado no HPA. O Load Balancer já tem o DNS externo publicado."

**[Mostrar o arquivo k8s/hpa.yaml no editor]**

> "O HPA escala entre 2 e 4 réplicas quando a CPU média ultrapassa 70%.
> Scale up aguarda 60 segundos, scale down aguarda 300 segundos para evitar flapping."

**[Abrir o Console AWS — EKS — mostrar o cluster siase-production]**

> "No console da AWS podemos ver o cluster EKS com o managed node group em 2 AZs.
> O RDS PostgreSQL está em subnet privada — não tem acesso público,
> só os pods do EKS conseguem conectar via endpoint interno."

**[Abrir o Console AWS — Secrets Manager — mostrar os segredos criados]**

> "As credenciais do banco, o segredo JWT e os tokens da aplicação estão no Secrets Manager.
> O CI/CD lê esses valores em tempo de deploy e os injeta como Kubernetes Secrets —
> nunca ficam expostos no repositório."

**[Abrir o GitHub Actions — aba Actions — mostrar o último run do deploy-prod.yml com sucesso]**

> "O CI/CD é feito pelo GitHub Actions. A cada push na main, o workflow:
> 1. Roda build e testes com cobertura mínima de 80% via JaCoCo
> 2. Autentica na AWS com credenciais temporárias do Learner Lab
> 3. Faz build e push da imagem para o ECR com tag do git SHA
> 4. Lê segredos do Secrets Manager e parâmetros do SSM
> 5. Aplica os manifestos K8s no cluster EKS
> 6. Aguarda o DNS do Load Balancer e salva no SSM para o frontend consumir"

**[Mostrar o step de deploy no log do GitHub Actions expandido]**

> "Uma observação importante: o Learner Lab não suporta OIDC.
> Por isso usamos credenciais temporárias que expiram a cada 4 horas de sessão.
> A cada nova sessão do Learner Lab, atualizamos os 3 secrets no GitHub manualmente."

**[Mostrar o Dockerfile no editor]**

> "O Dockerfile usa multi-stage build: estágio Maven compila os 3 módulos,
> estágio JRE é o runtime. Container roda como non-root com flags JVM
> container-aware: UseContainerSupport e MaxRAMPercentage=75,
> respeitando os limites de memória do pod no EKS."

---

## PARTE 7 — Testes e Qualidade (30 seg)

**[Mostrar no GitHub Actions o step de build-test com o log do JaCoCo, ou rodar localmente: ./mvnw test jacoco:report]**

> "Os testes seguem a mesma separação da arquitetura:
> - siase-domain: testes unitários de regras de negócio
> - siase-application: testes dos use cases com mocks dos ports
> - siase-infrastructure: testes de controller com MockMvc e banco H2 em memória
>
> A cobertura mínima de 80% de linhas é uma regra obrigatória do build via JaCoCo.
> O build falha se a cobertura cair abaixo desse limite."

---

## PARTE 8 — Encerramento (30 seg)

**[Voltar para o README.md ou diagrama de arquitetura]**

> "Para resumir o que foi entregue na Fase 3:
>
> - Segurança: API Gateway com Lambda Authorizer para autenticação de clientes,
>   JWT HS256, BCrypt, validação de CPF/CNPJ e placa Mercosul
>
> - Observabilidade: 5 métricas customizadas Micrometer, logs JSON estruturados
>   com correlationId, Prometheus + Grafana + Loki + Alertmanager no EKS
>
> - Operação: EKS gerenciado na AWS, HPA com escalonamento automático,
>   RDS PostgreSQL com KMS e Secrets Manager, CI/CD completo com GitHub Actions
>
> - Arquitetura: 4 repositórios independentes com CI/CD próprio,
>   Arquitetura Hexagonal com 3 módulos Maven e regra de dependência garantida pelo compilador
>
> O código, documentação arquitetural com ADRs e RFCs, e a collection Postman
> estão disponíveis no repositório. Obrigado!"

---

## Dicas de gravação

- Mantenha o terminal com fonte grande (mínimo 16px) para legibilidade
- Use `clear` antes de cada comando no terminal
- No Postman, troque a variável `baseUrl` para `http://<EXTERNAL-IP>/api` antes de gravar
- Mostre a resposta completa no Postman — não apenas o status HTTP
- Tenha o DNS do Load Balancer anotado antes de começar — ele demora alguns minutos para propagar após o deploy
- A sessão do Learner Lab dura 4 horas — inicie a gravação logo após abrir a sessão para não ter credenciais expiradas no meio
- Se o port-forward cair durante a gravação, reabra com o mesmo comando — é normal no Learner Lab
- Grave em partes se necessário e edite depois — não precisa ser uma tomada única
- Tempo ideal por parte: Apresentação (1min) + Arquitetura (1,5min) + Segurança (2min)
  + Fluxo OS (2,5min) + Observabilidade (2min) + K8s/CI/CD (1,5min) + Testes (30s) + Encerramento (30s)
