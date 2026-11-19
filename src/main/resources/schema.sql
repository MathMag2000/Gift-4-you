-- Esquema do banco PostgreSQL. Executado a cada inicialização (spring.sql.init.mode=always):
-- os comandos usam IF NOT EXISTS, então os dados existentes são mantidos.
-- Enums são guardados pelo código (ex.: AMIGO), o mesmo valor usado na API.

CREATE TABLE IF NOT EXISTS usuario (
    id         SERIAL PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    email      VARCHAR(254) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    criado_em  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pessoa (
    id               SERIAL PRIMARY KEY,
    usuario_id       INTEGER       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    nome             TEXT          NOT NULL,
    idade            INTEGER       NOT NULL CHECK (idade BETWEEN 0 AND 120),
    vinculo          VARCHAR(30)   NOT NULL,
    gostos           TEXT[]        NOT NULL DEFAULT '{}',
    nao_gosta        TEXT[]        NOT NULL DEFAULT '{}',
    ocasiao          VARCHAR(30)   NOT NULL,
    orcamento_maximo NUMERIC        NOT NULL CHECK (orcamento_maximo > 0)
);

CREATE INDEX IF NOT EXISTS idx_pessoa_usuario ON pessoa (usuario_id);

-- Campos adicionados depois da criação da tabela: bancos já existentes recebem as colunas aqui.
ALTER TABLE pessoa ADD COLUMN IF NOT EXISTS estilos     TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE pessoa ADD COLUMN IF NOT EXISTS observacoes TEXT   NOT NULL DEFAULT '';

-- Catálogo de presentes: cada conta tem o próprio.
CREATE TABLE IF NOT EXISTS presente (
    id              SERIAL PRIMARY KEY,
    usuario_id      INTEGER        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    nome            TEXT           NOT NULL,
    categoria       VARCHAR(30)    NOT NULL,
    descricao       TEXT           NOT NULL DEFAULT '',
    preco           NUMERIC        NOT NULL CHECK (preco > 0),
    caracteristicas TEXT[]         NOT NULL DEFAULT '{}',
    ocasioes        TEXT[]         NOT NULL DEFAULT '{}',
    link_compra     VARCHAR(2000)  NOT NULL,
    imagem_url      TEXT
);

-- Bancos criados quando o catálogo era compartilhado: os presentes existentes passam a ser da primeira
-- conta cadastrada. Sem nenhuma conta, não há dono possível e esses presentes são removidos.
ALTER TABLE presente ADD COLUMN IF NOT EXISTS usuario_id INTEGER REFERENCES usuario (id) ON DELETE CASCADE;
UPDATE presente SET usuario_id = (SELECT MIN(id) FROM usuario) WHERE usuario_id IS NULL;
DELETE FROM presente WHERE usuario_id IS NULL;
ALTER TABLE presente ALTER COLUMN usuario_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_presente_usuario ON presente (usuario_id);

-- Favoritos e histórico guardam uma cópia do item sugerido (ver ItemSugerido). Por isso presente_id
-- não é chave estrangeira: o favorito continua existindo se o presente sair do catálogo.
CREATE TABLE IF NOT EXISTS favorito (
    id             SERIAL PRIMARY KEY,
    pessoa_id      INTEGER     NOT NULL REFERENCES pessoa (id) ON DELETE CASCADE,
    origem         VARCHAR(20) NOT NULL,
    presente_id    INTEGER,
    nome           TEXT        NOT NULL,
    categoria      TEXT,
    preco          NUMERIC,
    motivos        TEXT[]      NOT NULL DEFAULT '{}',
    link_compra    TEXT,
    imagem_url     TEXT,
    favoritado_em  TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_favorito_pessoa ON favorito (pessoa_id);

CREATE TABLE IF NOT EXISTS registro_historico (
    id               SERIAL PRIMARY KEY,
    pessoa_id        INTEGER        NOT NULL REFERENCES pessoa (id) ON DELETE CASCADE,
    origem           VARCHAR(20)    NOT NULL,
    ocasiao          VARCHAR(30)    NOT NULL,
    orcamento_maximo NUMERIC        NOT NULL,
    realizado_em     TIMESTAMP      NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_registro_historico_pessoa ON registro_historico (pessoa_id);

CREATE TABLE IF NOT EXISTS item_historico (
    registro_id INTEGER        NOT NULL REFERENCES registro_historico (id) ON DELETE CASCADE,
    posicao     INTEGER        NOT NULL,
    presente_id INTEGER,
    nome        TEXT           NOT NULL,
    categoria   TEXT,
    preco       NUMERIC,
    motivos     TEXT[]         NOT NULL DEFAULT '{}',
    link_compra TEXT,
    imagem_url  TEXT,
    PRIMARY KEY (registro_id, posicao)
);
