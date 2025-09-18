CREATE TABLE IF NOT EXISTS members (
    id        CHAR(26)     NOT NULL,
    email     VARCHAR(255) NOT NULL,
    name      VARCHAR(255) NOT NULL,
    password  VARCHAR(255) NOT NULL,
    status    VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_members PRIMARY KEY (id),
    CONSTRAINT ux_members_email UNIQUE (email),
    CONSTRAINT ck_members_status CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED'))
);