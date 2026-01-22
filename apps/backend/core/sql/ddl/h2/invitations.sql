CREATE TABLE IF NOT EXISTS workspace_invitations (
    id            CHAR(26)     NOT NULL,
    workspace_id  CHAR(26)     NOT NULL,
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
    CONSTRAINT pk_workspace_invitations PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_workspace_inv_workspace_id ON workspace_invitations (workspace_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_workspace_inv_email_pending ON workspace_invitations (invited_email, status, deleted_at);

CREATE TABLE IF NOT EXISTS project_invitations (
    id            CHAR(26)     NOT NULL,
    project_id    CHAR(26)     NOT NULL,
    workspace_id  CHAR(26)     NOT NULL,
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
    CONSTRAINT pk_project_invitations PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_project_inv_project_id ON project_invitations (project_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_project_inv_email_pending ON project_invitations (invited_email, status, deleted_at);
