ALTER TABLE itens_servico
    ADD COLUMN IF NOT EXISTS data_inicio_execucao TIMESTAMP;

ALTER TABLE itens_servico
    ADD COLUMN IF NOT EXISTS data_fim_execucao TIMESTAMP;
