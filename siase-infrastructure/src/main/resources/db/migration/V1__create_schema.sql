-- =====================================================
-- SIASE - Sistema Integrado de Atendimento e Execução de Serviços
-- Migração inicial do schema
-- =====================================================

-- Tabela de exemplo para validar a configuração do Flyway
CREATE TABLE IF NOT EXISTS schema_info (
    id         BIGSERIAL PRIMARY KEY,
    versao     VARCHAR(20) NOT NULL,
    descricao  VARCHAR(255),
    criado_em  TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO schema_info (versao, descricao) VALUES ('1.0.0', 'Schema inicial criado');
