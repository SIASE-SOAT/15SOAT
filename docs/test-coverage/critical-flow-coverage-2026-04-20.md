# Critical Flow Coverage Evidence

## Baseline

| Class | Current signal |
| --- | --- |
| `Agendamento` | near 0% |
| `AgendamentoService` | near 0% |
| `PagamentoController` | 0% |
| `PedidoCompraController` | 0% |

## Target

- Reach at least 80% line coverage for the critical classes touched in this round.
- Preserve existing passing coverage on `OrdemDeServicoService`, `PagamentoService`, and `PecaService`.

## Execution order

1. `AgendamentoEntityTest`
2. `AgendamentoServiceTest`
3. `PagamentoControllerTest`
4. `PedidoCompraControllerTest`
5. Full suite + JaCoCo evidence refresh

## Final

| Class | Line coverage (measured) |
| --- | --- |
| `Agendamento` | 100% (13/13) |
| `AgendamentoService` | 86.1% (31/36) |
| `PagamentoController` | 100% (9/9) |
| `PedidoCompraController` | 100% (14/14) |

## Verification commands

- Focused: `-Dtest=AgendamentoEntityTest,AgendamentoServiceTest,PagamentoControllerTest,PedidoCompraControllerTest test`
- Full suite: `test`

## Notes

- Focused regression completed with success (`Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`).
- To preserve expected API contract for parameter validation (`@Min` in query params), `GlobalExceptionHandler` now handles `HandlerMethodValidationException` as HTTP 400.
