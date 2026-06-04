-- Relação entre serviços e peças/insumos necessários para sua execução
CREATE TABLE servico_insumos (
    servico_id  UUID    NOT NULL,
    peca_id     UUID    NOT NULL,
    quantidade  INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT pk_servico_insumos    PRIMARY KEY (servico_id, peca_id),
    CONSTRAINT fk_si_servico         FOREIGN KEY (servico_id) REFERENCES servicos (id),
    CONSTRAINT fk_si_peca            FOREIGN KEY (peca_id)    REFERENCES pecas (id),
    CONSTRAINT ck_si_quantidade      CHECK (quantidade > 0)
);

CREATE INDEX idx_servico_insumos_servico ON servico_insumos (servico_id);
CREATE INDEX idx_servico_insumos_peca    ON servico_insumos (peca_id);
