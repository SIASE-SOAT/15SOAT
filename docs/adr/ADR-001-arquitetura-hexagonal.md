# ADR-001 — Adocao de Arquitetura Hexagonal (Ports and Adapters)

**Status:** Aceito
**Data:** 2026
**Repositorio:** 15SOAT (siase-app)

## Contexto

Na Fase 1, a aplicacao foi desenvolvida como um monolito em camadas (controller → service → repository). Na Fase 2, o desafio exigiu refatoracao para Clean Architecture ou Arquitetura Hexagonal. A escolha da abordagem arquitetural impacta diretamente a testabilidade, a manutencao e a evolucao do sistema.

## Decisao

Adotar **Arquitetura Hexagonal (Ports and Adapters)** organizada em **3 modulos Maven independentes**:

- `siase-domain`: entidades POJO, enums, interfaces de porta, validacoes de dominio.
- `siase-application`: use cases, DTOs, interfaces de porta de entrada.
- `siase-infrastructure`: adaptadores JPA, controllers REST, seguranca JWT, email, observabilidade.

## Justificativa

- **Inversao de dependencia:** `infrastructure → application → domain`. O dominio nao conhece frameworks. O compilador impede violacoes de arquitetura.
- **Testabilidade:** use cases sao testados com mocks dos ports, sem necessidade de banco ou servidor HTTP. Controllers sao testados com MockMvc e H2 em memoria.
- **Evolucao independente:** trocar o banco de dados, o framework HTTP ou o provedor de email exige apenas um novo adaptador, sem tocar no dominio ou nos use cases.
- **Linguagem Ubiqua:** as classes do dominio (`OrdemDeServico`, `Peca`, `Cliente`) refletem diretamente o vocabulario da oficina mecanica, facilitando a comunicacao com o negocio.
- **Modulos Maven:** a separacao em modulos garante que o compilador valide as dependencias em tempo de build, nao apenas em tempo de execucao.

## Alternativas Consideradas

| Alternativa              | Motivo da Rejeicao                                                          |
|--------------------------|-----------------------------------------------------------------------------|
| Camadas tradicionais     | Acoplamento entre logica de negocio e frameworks; dificuldade de teste      |
| Microservicos            | Complexidade operacional desproporcional ao escopo; sem requisito de escala |
| Clean Architecture (Uncle Bob) | Muito similar; Hexagonal e mais direto para o contexto de APIs REST  |

## Consequencias

- O `siase-domain` nao tem dependencias de Spring, JPA ou qualquer framework.
- O `siase-application` depende apenas do `siase-domain`.
- O `siase-infrastructure` e o unico modulo com dependencias de Spring Boot, JPA, JWT e MapStruct.
- A cobertura minima de 80% e verificada pelo JaCoCo no build, com exclusoes explicitas para classes de configuracao e DTOs.
