# Design: Cobertura de Testes dos Fluxos Críticos

## Contexto

O projeto já possui uma suíte relevante de testes unitários e de integração no backend Spring Boot, com cobertura forte em serviços como ordem de serviço, peças, serviços, clientes e segurança. Mesmo assim, a análise do relatório JaCoCo atual mostra lacunas importantes justamente em partes que impactam a jornada ponta a ponta da oficina, especialmente agendamento e alguns controllers ligados a pagamento e pedido de compra.

O objetivo desta rodada é fortalecer a evidência de qualidade nos domínios críticos do negócio, sem dispersar esforço em cobertura cosmética. A prioridade será aumentar a proteção automatizada sobre a jornada principal da oficina e, como consequência, elevar ou ao menos sustentar a cobertura global com base em cenários de negócio.

## Objetivo

Garantir testes automatizados robustos para os principais fluxos da oficina, com evidência de cobertura mínima de 80% nos domínios críticos e melhoria sustentável da cobertura global do backend.

## Escopo

### No escopo

- Revisar a cobertura atual do backend com base no relatório JaCoCo.
- Tratar como domínios críticos os fluxos de:
  - agendamento
  - ordem de serviço
  - estoque e peças
  - pagamento
- Cobrir tanto regras de negócio quanto endpoints que sustentam a jornada crítica quando houver lacunas relevantes.
- Adicionar testes unitários e de integração.
- Fazer pequenos ajustes no código de produção apenas quando forem necessários para viabilizar testabilidade ou corrigir comportamento revelado pelos testes.
- Rerodar a suíte com JaCoCo ao final para gerar evidência objetiva.

### Fora do escopo

- Refatorações amplas de arquitetura.
- Reescrita de módulos já bem cobertos apenas para aumentar número de cobertura.
- Tratamento do frontend nesta rodada.

## Estado Atual Resumido

Com base no relatório atual de cobertura:

- `AgendamentoService` está sem cobertura prática.
- `Agendamento` está sem cobertura prática.
- `PedidoCompraController` está sem cobertura.
- `PagamentoController` está sem cobertura.
- `EmailService` está com cobertura residual, mas pode ser coberto indiretamente pelos fluxos que o utilizam ou permanecer com cobertura baixa se for apenas efeito colateral sem valor de negócio.
- `OrdemDeServicoService`, `PecaService` e `PagamentoService` já possuem boa base, mas ainda têm pontos não cobertos que podem ser complementados se estiverem dentro da jornada crítica.

## Abordagens Consideradas

### 1. Cobrir apenas classes zeradas

Prós:
- Mais rápida para subir o número.

Contras:
- Pode gerar cobertura artificial.
- Não necessariamente fortalece os fluxos mais importantes do negócio.

### 2. Cobrir a jornada crítica da oficina

Prós:
- Gera proteção real sobre o que mais importa.
- Produz evidência melhor para entrega e avaliação.
- Tende a elevar a cobertura global de forma sustentável.

Contras:
- Exige escolha cuidadosa de cenários e ordem de ataque.

### 3. Subir cobertura global genericamente

Prós:
- Pode melhorar o indicador agregado.

Contras:
- Espalha esforço em áreas menos relevantes.
- Tem maior risco de criar testes de baixo valor.

## Abordagem Escolhida

Seguir a abordagem 2: cobrir a jornada crítica da oficina.

## Design da Solução

### Estratégia de execução

O trabalho será guiado por análise de lacunas reais de cobertura e por TDD para cada novo comportamento coberto. A ordem de execução será orientada pelo maior retorno em fluxo crítico:

1. Consolidar a linha de base da cobertura atual.
2. Atacar `AgendamentoService` e `Agendamento`, porque hoje representam o maior vazio na jornada do cliente até a oficina.
3. Cobrir `PagamentoController` e `PedidoCompraController`, porque expõem fluxos administrativos importantes ainda sem proteção automatizada.
4. Complementar cenários residuais em `PagamentoService`, `PecaService` ou `OrdemDeServicoService` apenas se forem necessários para atingir a meta de 80% nos domínios críticos.
5. Executar a suíte completa com relatório de cobertura e documentar o resultado final.

### Tipos de teste

- Testes unitários para regras de transição de estado, validações, exceções e interações de serviço.
- Testes de integração ou de controller para contratos HTTP, códigos de status, serialização e integração com camada web.
- Uso de mocks apenas quando inevitável para dependências externas ou laterais, como envio de e-mail.

### Critério de valor

Cada teste novo deve responder pelo menos a uma destas perguntas:

- O fluxo crítico funciona no caminho feliz?
- O fluxo crítico bloqueia uma transição inválida?
- O endpoint devolve o contrato esperado?
- Uma regressão relevante seria detectada por esse teste?

Se a resposta for não, o teste não entra.

## Fluxos Prioritários

### Agendamento

- Criar agendamento com sucesso.
- Listar agendamentos.
- Filtrar por status.
- Filtrar por cliente.
- Confirmar agendamento válido.
- Cancelar agendamento válido.
- Rejeitar transições inválidas no domínio.
- Verificar interação esperada com envio de confirmação.

### Pagamento

- Registrar pagamento.
- Confirmar pagamento.
- Cancelar pagamento.
- Buscar pagamento por ordem de serviço.
- Validar endpoints do controller para os cenários principais.

### Estoque e peças

- Manter cobertura das regras de movimentação e consistência de estoque.
- Complementar apenas lacunas que impactem diretamente a jornada de execução da ordem de serviço.

### Ordem de serviço

- Preservar a robustez atual.
- Complementar apenas cenários necessários para fechar a cadeia com pagamento, peças ou serviços.

## Riscos e Mitigações

- Risco: perseguir cobertura numérica sem ganho real.
  Mitigação: priorizar cenários de negócio e classes da jornada crítica.

- Risco: testes frágeis por excesso de mocking.
  Mitigação: preferir comportamento observável e uso pontual de mocks.

- Risco: descobrir comportamento ambíguo ou incorreto no código atual.
  Mitigação: permitir pequenos ajustes no código de produção quando o teste revelar problema real.

- Risco: cobertura global já ser suficiente, mas domínios críticos seguirem desbalanceados.
  Mitigação: medir e reportar cobertura por classe e por fluxo prioritário, não apenas o total agregado.

## Verificação

Ao final, a validação será feita por:

- execução fresca de `mvn test`
- geração fresca do relatório JaCoCo
- leitura dos percentuais das classes e fluxos críticos tratados
- confirmação de que os domínios críticos atingiram ou superaram 80% de cobertura de linha, sempre que aplicável ao escopo escolhido

## Resultado Esperado

Ao fim desta rodada, o backend terá:

- maior proteção automatizada sobre a jornada crítica da oficina
- evidência objetiva de cobertura sobre agendamento, pagamento, estoque e ordem de serviço
- base mais segura para encerramento do projeto e futuras refatorações, incluindo a migração arquitetural planejada
