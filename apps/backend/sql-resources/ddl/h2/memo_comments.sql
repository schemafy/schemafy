CREATE TABLE IF NOT EXISTS memo_comments (
    id         CHAR(26)  NOT NULL,
    memo_id    CHAR(26)  NOT NULL,
    author_id  CHAR(26)  NOT NULL,
    body       TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT pk_memo_comments PRIMARY KEY (id)
);

