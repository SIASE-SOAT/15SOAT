-- =====================================================
-- SIASE - Fluxo Completo da Oficina
-- Adiciona: CANCELADA no status OS, Agendamentos,
--           Pagamentos e Pedidos de Compra de Peças
-- =====================================================

-- Atualiza constraint do status da OS para incluir CANCELADA
ALTER TABLE ordens_de_servico DROP CONSTRAINT IF EXISTS ck_os_status;
ALTER TABLE ordens_de_servico ADD CONSTRAINT ck_os_status CHECK (status IN (
    'RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO',
    'EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE', 'CANCELADA'
));

-- Agendamentos (pré-OS, feitos pelo atendente com o cliente)
CREATE TABLE agendamentos (
    id                  UUID         NOT NULL,
    cliente_id          UUID         NOT NULL,
    veiculo_id          UUID         NOT NULL,
    data_hora           TIMESTAMP    NOT NULL,
    descricao_servicos  TEXT,
    status              VARCHAR(20)  NOT NULL DEFAULT 'AGENDADO',
    ordem_de_servico_id UUID,
    criado_em           TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_agendamento_cliente FOREIGN KEY (cliente_id)          REFERENCES clientes (id),
    CONSTRAINT fk_agendamento_veiculo FOREIGN KEY (veiculo_id)          REFERENCES veiculos (id),
    CONSTRAINT fk_agendamento_os      FOREIGN KEY (ordem_de_servico_id) REFERENCES ordens_de_servico (id),
    CONSTRAINT ck_agendamento_status  CHECK (status IN ('AGENDADO', 'CONFIRMADO', 'CANCELADO', 'REALIZADO'))
);

-- Pagamentos de OS (registrado após execução do serviço)
CREATE TABLE pagamentos (
    id                  UUID          NOT NULL,
    ordem_de_servico_id UUID          NOT NULL,
    forma_pagamento     VARCHAR(30)   NOT NULL,
    valor               NUMERIC(10,2) NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    data_pagamento      TIMESTAMP,
    criado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uq_pagamento_os        UNIQUE (ordem_de_servico_id),
    CONSTRAINT fk_pagamento_os        FOREIGN KEY (ordem_de_servico_id) REFERENCES ordens_de_servico (id),
    CONSTRAINT ck_pagamento_forma     CHECK (forma_pagamento IN (
        'DINHEIRO', 'CARTAO_DEBITO', 'CARTAO_CREDITO', 'PIX', 'TRANSFERENCIA'
    )),
    CONSTRAINT ck_pagamento_status    CHECK (status IN ('PENDENTE', 'PAGO', 'CANCELADO'))
);

-- Pedidos de Compra de Peças (quando estoque está zerado)
CREATE TABLE pedidos_compra (
    id                    UUID         NOT NULL,
    peca_id               UUID         NOT NULL,
    quantidade_solicitada INTEGER      NOT NULL,
    quantidade_recebida   INTEGER      NOT NULL DEFAULT 0,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    observacoes           TEXT,
    criado_em             TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em         TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_pedido_compra_peca FOREIGN KEY (peca_id) REFERENCES pecas (id),
    CONSTRAINT ck_pedido_status      CHECK (status IN ('PENDENTE', 'APROVADO', 'RECEBIDO', 'CANCELADO'))
);

-- Índices
CREATE INDEX idx_agendamentos_cliente  ON agendamentos (cliente_id);
CREATE INDEX idx_agendamentos_veiculo  ON agendamentos (veiculo_id);
CREATE INDEX idx_agendamentos_data     ON agendamentos (data_hora);
CREATE INDEX idx_agendamentos_status   ON agendamentos (status);
CREATE INDEX idx_pedidos_compra_peca   ON pedidos_compra (peca_id);
CREATE INDEX idx_pedidos_compra_status ON pedidos_compra (status);
