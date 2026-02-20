CREATE TABLE IF NOT EXISTS db_schemas (
    id              CHAR(26)     NOT NULL,
    project_id      CHAR(26)     NOT NULL,
    db_vendor_name  VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    charset         VARCHAR(64)  NULL,
    collation       VARCHAR(64)  NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP    NULL,
    CONSTRAINT pk_db_schemas PRIMARY KEY (id)
);

ALTER TABLE db_schemas
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
