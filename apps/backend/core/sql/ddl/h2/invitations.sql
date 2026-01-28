CREATE TABLE IF NOT EXISTS invitations (
    id            CHAR(26)     NOT NULL,
    target_type   VARCHAR(20)  NOT NULL,
    target_id     CHAR(26)     NOT NULL,
    parent_id     CHAR(26)     NULL,
    invited_email VARCHAR(255) NOT NULL,
    invited_role  VARCHAR(32)  NOT NULL,
    invited_by    CHAR(26)     NOT NULL,
    status        VARCHAR(32)  NOT NULL DEFAULT 'pending',
    expires_at    TIMESTAMP    NOT NULL,
    resolved_at   TIMESTAMP    NULL,
    version       INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP    NULL,
    CONSTRAINT pk_invitations PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_inv_target_email_status ON invitations (target_type, target_id, invited_email, status, deleted_at);
CREATE INDEX IF NOT EXISTS idx_inv_email_type_status ON invitations (invited_email, target_type, status, deleted_at);
