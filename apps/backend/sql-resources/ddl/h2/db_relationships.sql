CREATE TABLE IF NOT EXISTS db_relationships (
    id          CHAR(26)     NOT NULL,
    pk_table_id CHAR(26)     NOT NULL,
    fk_table_id CHAR(26)     NOT NULL,
    name        VARCHAR(255) NOT NULL,
    kind        VARCHAR(32)  NOT NULL,
    cardinality VARCHAR(16)  NOT NULL,
    extra       JSON         NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP    NULL,
    CONSTRAINT pk_db_relationships PRIMARY KEY (id)
);
