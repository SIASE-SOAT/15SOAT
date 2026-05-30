-- =====================================================
-- SIASE - Sistema Integrado de Atendimento e Execução de Serviços
-- Schema principal do domínio
-- =====================================================

-- Clientes (pessoa física ou jurídica)
CREATE TABLE clientes (
    id              UUID         NOT NULL,
    nome            VARCHAR(255) NOT NULL,
    tipo_pessoa     VARCHAR(2)   NOT NULL CHECK (tipo_pessoa IN ('PF', 'PJ')),
    documento       VARCHAR(18)  NOT NULL,
    email           VARCHAR(255),
    telefone        VARCHAR(20),
    endereco        VARCHAR(500),
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uq_clientes_documento UNIQUE (documento)
);

-- Veículos associados a clientes
CREATE TABLE veiculos (
    id              UUID         NOT NULL,
    placa           VARCHAR(10)  NOT NULL,
    marca           VARCHAR(100) NOT NULL,
    modelo          VARCHAR(100) NOT NULL,
    ano             INTEGER      NOT NULL,
    cor             VARCHAR(50),
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    cliente_id      UUID         NOT NULL,
    criado_em       TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uq_veiculos_placa    UNIQUE (placa),
    CONSTRAINT fk_veiculos_cliente  FOREIGN KEY (cliente_id) REFERENCES clientes (id)
);

-- Catálogo de serviços oferecidos
CREATE TABLE servicos (
    id                      UUID          NOT NULL,
    nome                    VARCHAR(255)  NOT NULL,
    descricao               TEXT,
    preco                   NUMERIC(10,2) NOT NULL,
    tempo_estimado_minutos  INTEGER,
    ativo                   BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em               TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id)
);

-- Peças e insumos com controle de estoque
CREATE TABLE pecas (
    id                  UUID          NOT NULL,
    codigo              VARCHAR(50)   NOT NULL,
    nome                VARCHAR(255)  NOT NULL,
    descricao           TEXT,
    preco               NUMERIC(10,2) NOT NULL,
    quantidade_estoque  INTEGER       NOT NULL DEFAULT 0,
    estoque_minimo      INTEGER       NOT NULL DEFAULT 0,
    unidade_medida      VARCHAR(20)   NOT NULL DEFAULT 'UN',
    ativo               BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uq_pecas_codigo UNIQUE (codigo)
);

-- Ordens de Serviço
CREATE TABLE ordens_de_servico (
    id              UUID          NOT NULL,
    numero          VARCHAR(20)   NOT NULL,
    cliente_id      UUID          NOT NULL,
    veiculo_id      UUID          NOT NULL,
    status          VARCHAR(30)   NOT NULL DEFAULT 'RECEBIDA',
    observacoes     TEXT,
    total_servicos  NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_pecas     NUMERIC(10,2) NOT NULL DEFAULT 0,
    total           NUMERIC(10,2) NOT NULL DEFAULT 0,
    data_abertura   TIMESTAMP     NOT NULL DEFAULT NOW(),
    data_fechamento TIMESTAMP,
    criado_em       TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uq_os_numero         UNIQUE (numero),
    CONSTRAINT fk_os_cliente        FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_os_veiculo        FOREIGN KEY (veiculo_id) REFERENCES veiculos (id),
    CONSTRAINT ck_os_status         CHECK (status IN (
        'RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO',
        'EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE'
    ))
);

-- Serviços incluídos em uma OS
CREATE TABLE itens_servico (
    id                      UUID          NOT NULL,
    ordem_de_servico_id     UUID          NOT NULL,
    servico_id              UUID          NOT NULL,
    preco_unitario          NUMERIC(10,2) NOT NULL,
    tempo_estimado_minutos  INTEGER,
    observacoes             TEXT,
    criado_em               TIMESTAMP     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_itens_servico_os      FOREIGN KEY (ordem_de_servico_id) REFERENCES ordens_de_servico (id),
    CONSTRAINT fk_itens_servico_servico FOREIGN KEY (servico_id)          REFERENCES servicos (id)
);

-- Peças incluídas em uma OS
CREATE TABLE itens_peca (
    id                  UUID          NOT NULL,
    ordem_de_servico_id UUID          NOT NULL,
    peca_id             UUID          NOT NULL,
    quantidade          INTEGER       NOT NULL,
    preco_unitario      NUMERIC(10,2) NOT NULL,
    criado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_itens_peca_os   FOREIGN KEY (ordem_de_servico_id) REFERENCES ordens_de_servico (id),
    CONSTRAINT fk_itens_peca_peca FOREIGN KEY (peca_id)             REFERENCES pecas (id)
);

-- Índices para consultas frequentes
CREATE INDEX idx_clientes_documento        ON clientes (documento);
CREATE INDEX idx_veiculos_placa            ON veiculos (placa);
CREATE INDEX idx_veiculos_cliente          ON veiculos (cliente_id);
CREATE INDEX idx_os_cliente                ON ordens_de_servico (cliente_id);
CREATE INDEX idx_os_veiculo                ON ordens_de_servico (veiculo_id);
CREATE INDEX idx_os_status                 ON ordens_de_servico (status);
CREATE INDEX idx_itens_servico_os          ON itens_servico (ordem_de_servico_id);
CREATE INDEX idx_itens_peca_os             ON itens_peca (ordem_de_servico_id);
