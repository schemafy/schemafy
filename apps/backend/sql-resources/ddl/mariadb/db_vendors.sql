CREATE TABLE IF NOT EXISTS db_vendors (
    id                INT          NOT NULL AUTO_INCREMENT,
    display_name      VARCHAR(255) NOT NULL,
    name              VARCHAR(64)  NOT NULL,
    version           VARCHAR(64)  NOT NULL,
    datatype_mappings JSON         NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP    NULL,
    CONSTRAINT pk_db_vendors PRIMARY KEY (id),
    CONSTRAINT uq_db_vendors_name_version UNIQUE (name, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
