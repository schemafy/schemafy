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
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP    NULL,
    CONSTRAINT pk_workspace_invitations PRIMARY KEY (id),
    INDEX idx_workspace_inv_workspace_id (workspace_id, deleted_at),
    INDEX idx_workspace_inv_email_pending (invited_email, status, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP    NULL,
    CONSTRAINT pk_project_invitations PRIMARY KEY (id),
    INDEX idx_project_inv_project_id (project_id, deleted_at),
    INDEX idx_project_inv_email_pending (invited_email, status, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
