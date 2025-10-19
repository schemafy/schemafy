CREATE TABLE IF NOT EXISTS db_relationships (
    id           CHAR(26)     NOT NULL,
    src_table_id CHAR(26)     NOT NULL,
    tgt_table_id CHAR(26)     NOT NULL,
    name         VARCHAR(255) NOT NULL,
    kind         VARCHAR(32)  NOT NULL,
    cardinality  VARCHAR(16)  NOT NULL,
    on_delete    VARCHAR(16)  NOT NULL,
    on_update    VARCHAR(16)  NOT NULL,
    extra        TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP    NULL,
    CONSTRAINT pk_db_relationships PRIMARY KEY (id)
);

