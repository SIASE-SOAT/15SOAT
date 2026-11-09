# RFC-002 — Integração com Mercado Pago no Billing Service

**Status:** Aprovado  
**Data:** 2025-01

## Resumo

Define a estratégia de integração com o Mercado Pago para processamento de pagamentos das OS finalizadas.

## Fluxo de Pagamento

```
1. OS atinge status FINALIZADA
2. Execution Service publica EXECUCAO_FINALIZADA
3. Billing Service consome o evento
4. Billing cria preferência de pagamento no Mercado Pago
5. Mercado Pago retorna link de pagamento
6. Billing notifica cliente com o link (email/SMS)
7. Cliente realiza pagamento
8. Mercado Pago chama webhook do Billing Service
9. Billing confirma pagamento e publica PAGAMENTO_CONFIRMADO
10. OS Service consome e avança OS para ENTREGUE
```

## Endpoints Mercado Pago Utilizados

| Endpoint | Método | Propósito |
|---|---|---|
| `/checkout/preferences` | POST | Criar preferência de pagamento |
| `/v1/payments/{id}` | GET | Consultar status do pagamento |
| Webhook `/billing/webhook/mercadopago` | POST | Receber notificações |

## Configuração

```yaml
mercadopago:
  access-token: ${MERCADOPAGO_ACCESS_TOKEN}
  webhook-secret: ${MERCADOPAGO_WEBHOOK_SECRET}
  notification-url: ${BILLING_PUBLIC_URL}/api/billing/webhook/mercadopago
  success-url: ${PORTAL_URL}/pagamento/sucesso
  failure-url: ${PORTAL_URL}/pagamento/falha
```

## Segurança do Webhook

O webhook do Mercado Pago é validado via HMAC-SHA256 usando o `webhook-secret`. Requisições sem assinatura válida retornam 401.

## Tratamento de Falhas

- Pagamento recusado: Billing notifica cliente e mantém OS em FINALIZADA para nova tentativa
- Webhook não recebido: Billing faz polling a cada 5 minutos por até 24h
- Timeout Mercado Pago: retry com backoff exponencial (3 tentativas)
