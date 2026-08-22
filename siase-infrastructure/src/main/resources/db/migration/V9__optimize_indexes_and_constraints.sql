-- Indices redundantes: as constraints UNIQUE ja criam indice equivalente.
DROP INDEX IF EXISTS idx_clientes_documento;
DROP INDEX IF EXISTS idx_veiculos_placa;

-- Consulta de OS por periodo (volume diario) e por status ordenado por data.
CREATE INDEX IF NOT EXISTS idx_os_data_abertura ON ordens_de_servico (data_abertura DESC);
CREATE INDEX IF NOT EXISTS idx_os_status_data_abertura ON ordens_de_servico (status, data_abertura DESC);

-- Chaves estrangeiras sem indice penalizam join e validacao de integridade.
CREATE INDEX IF NOT EXISTS idx_itens_servico_servico ON itens_servico (servico_id);
CREATE INDEX IF NOT EXISTS idx_itens_peca_peca ON itens_peca (peca_id);

-- Itens com execucao iniciada e ainda nao finalizada.
CREATE INDEX IF NOT EXISTS idx_itens_servico_em_execucao
    ON itens_servico (ordem_de_servico_id)
    WHERE data_inicio_execucao IS NOT NULL AND data_fim_execucao IS NULL;

-- Consistencia dos timestamps de execucao do item.
ALTER TABLE itens_servico DROP CONSTRAINT IF EXISTS ck_itens_servico_execucao;
ALTER TABLE itens_servico ADD CONSTRAINT ck_itens_servico_execucao CHECK (
    data_fim_execucao IS NULL
        OR (data_inicio_execucao IS NOT NULL AND data_fim_execucao >= data_inicio_execucao)
    );

-- Totais monetarios nao podem ser negativos.
ALTER TABLE ordens_de_servico DROP CONSTRAINT IF EXISTS ck_os_totais_nao_negativos;
ALTER TABLE ordens_de_servico ADD CONSTRAINT ck_os_totais_nao_negativos CHECK (
    total_servicos >= 0 AND total_pecas >= 0 AND total >= 0
    );

-- Fechamento nao pode anteceder a abertura.
ALTER TABLE ordens_de_servico DROP CONSTRAINT IF EXISTS ck_os_datas;
ALTER TABLE ordens_de_servico ADD CONSTRAINT ck_os_datas CHECK (
    data_fechamento IS NULL OR data_fechamento >= data_abertura
    );
