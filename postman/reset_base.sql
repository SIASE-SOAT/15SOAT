-- =====================================================
-- SIASE — Reset completo da base para testes
-- Execute este script no seu cliente PostgreSQL
-- (DBeaver, psql, IntelliJ DataGrip, etc.)
--
-- O que é zerado:
--   - Todos os dados de negócio (OS, pagamentos, pedidos,
--     agendamentos, itens, clientes, veículos, peças, serviços)
--   - Usuários do sistema
--
-- O que é preservado:
--   - Estrutura das tabelas (DDL intacto)
--   - Histórico de migrations do Flyway
--
-- ⚠️  ATENÇÃO — APÓS EXECUTAR ESTE SCRIPT:
--   Limpe a variável {{jwtToken}} no Postman antes de continuar.
--   O token salvo aponta para um usuário que não existe mais.
--   Caso contrário, qualquer requisição que envie o header
--   Authorization retornará 403.
--
--   Como limpar no Postman:
--   1. Clique na coleção SIASE → aba "Variables"
--   2. Apague o valor de "jwtToken" (deixe em branco)
--   3. Clique em Save
--
--   Como limpar no Insomnia:
--   1. Abra o painel "Environments" (ícone de engrenagem ou menu Environment)
--   2. Selecione o environment da coleção SIASE
--   3. Apague o valor da variável "jwtToken" (deixe em branco)
--   4. Clique em Done / Save
--
--   Depois (Postman ou Insomnia):
--   Execute "Registrar usuário" e em seguida "Login"
--   para obter um token válido novo.
-- =====================================================

-- Desativa temporariamente as foreign keys para truncar na ordem certa
-- (CASCADE cuida das dependências automaticamente)

TRUNCATE TABLE
    pedidos_compra,
    pagamentos,
    agendamentos,
    itens_peca,
    itens_servico,
    ordens_de_servico,
    servico_insumos,
    veiculos,
    clientes,
    servicos,
    pecas,
    usuarios
CASCADE;
