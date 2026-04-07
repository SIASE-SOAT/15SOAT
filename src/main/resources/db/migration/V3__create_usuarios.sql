CREATE TABLE IF NOT EXISTS usuarios (
    id          UUID         NOT NULL,
    username    VARCHAR(100) NOT NULL,
    senha_hash  VARCHAR(255) NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_username UNIQUE (username)
);

INSERT INTO usuarios (id, username, senha_hash, ativo, criado_em) VALUES
    (gen_random_uuid(), 'atendente1', '$2a$10$p3TuBbxCMzJmBYqo7A2MbuDN0nqNBkKhGQHh294XxgyN0TWyD5qDK', true, NOW()),
    (gen_random_uuid(), 'atendente2', '$2a$10$kX0DgQP7zkxcI9FOsAbmuuhj1f30Cn0wNPuExLQEznY0DwrWMofgm', true, NOW())
ON CONFLICT (username) DO NOTHING;
