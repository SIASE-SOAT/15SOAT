CREATE TABLE IF NOT EXISTS usuarios (
    id          UUID         NOT NULL,
    username    VARCHAR(100) NOT NULL,
    senha_hash  VARCHAR(255) NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_username UNIQUE (username)
);
