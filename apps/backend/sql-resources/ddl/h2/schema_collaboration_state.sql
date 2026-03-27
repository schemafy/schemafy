CREATE TABLE IF NOT EXISTS schema_collaboration_state (
    schema_id         CHAR(26)   NOT NULL,
    project_id        CHAR(26)   NOT NULL,
    current_revision  BIGINT     NOT NULL DEFAULT 0,
    created_at        TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version           BIGINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_schema_collaboration_state PRIMARY KEY (schema_id)
);
