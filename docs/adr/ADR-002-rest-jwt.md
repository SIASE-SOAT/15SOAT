# ADR-002 — Padrao de Comunicacao REST e Autenticacao JWT

**Status:** Aceito
**Data:** 2026
**Repositorio:** 15SOAT (siase-app)

## Contexto

A aplicacao precisa expor APIs para dois tipos de consumidores: usuarios administrativos (mecanicos) e clientes externos. Cada tipo tem requisitos de autenticacao diferentes. E necessario definir o padrao de comunicacao e a estrategia de autenticacao para cada caso.

## Decisao

Adotar **APIs RESTful** com autenticacao **JWT Bearer** para usuarios administrativos e acesso **publico sem autenticacao** para o portal de acompanhamento do cliente. Na Fase 3, o token de cliente e emitido pela Lambda de autenticacao e validado pelo Lambda Authorizer do API Gateway antes de chegar a aplicacao.

## Justificativa

### REST

- Padrao amplamente adotado, bem suportado pelo Spring Boot e documentavel via Springdoc OpenAPI.
- Stateless por natureza, compativel com escalabilidade horizontal (HPA).
- Facilita o consumo pelo frontend Angular e por ferramentas como Postman.

### JWT Bearer

- Stateless: o servidor nao precisa armazenar sessoes, compativel com multiplas replicas.
- O token carrega as claims necessarias (`sub`, `roles`, `clienteId`) sem consulta ao banco a cada requisicao.
- CSRF nao e aplicavel: tokens JWT sao enviados via header `Authorization`, nunca via cookie automatico do browser.
- BCrypt para senhas: custo computacional configuravel, resistente a ataques de forca bruta.

### Separacao de autenticacao por tipo de usuario

| Tipo de usuario | Mecanismo                          | Endpoint                        |
|-----------------|------------------------------------|---------------------------------|
| Mecanico/Admin  | JWT emitido pelo Spring Boot       | `POST /api/auth/login`          |
| Cliente         | JWT emitido pela Lambda (Fase 3)   | `POST /auth/token` (API Gateway)|
| Portal publico  | Sem autenticacao                   | `GET /api/ordens/acompanhar/{n}`|

### Webhook para atualizacao de status

O endpoint `POST /api/ordens/webhook/status` usa autenticacao por token estatico (`WEBHOOK_TOKEN`) em vez de JWT, pois e consumido por sistemas externos que nao possuem credenciais de usuario.

## Alternativas Consideradas

| Alternativa       | Motivo da Rejeicao                                                          |
|-------------------|-----------------------------------------------------------------------------|
| Session/Cookie    | Stateful; incompativel com escalabilidade horizontal                        |
| OAuth2/OpenID     | Complexidade desnecessaria para o escopo; sem provedor de identidade externo|
| API Key estatica  | Menos seguro que JWT; sem expiracao automatica                              |
| GraphQL           | Overhead de implementacao; REST e suficiente para os casos de uso           |

## Consequencias

- Todos os endpoints administrativos exigem `Authorization: Bearer <token>` no header.
- O endpoint `/api/ordens/acompanhar/{numero}` e publico e nao exige autenticacao.
- O `SecurityConfig` usa `SessionCreationPolicy.STATELESS` — nenhuma sessao HTTP e criada.
- O `JwtAuthenticationFilter` valida o token antes de cada requisicao autenticada.
- Na Fase 3, o Lambda Authorizer valida o token de cliente antes de encaminhar para a aplicacao, sem que a aplicacao precise conhecer a logica de autenticacao de clientes.
