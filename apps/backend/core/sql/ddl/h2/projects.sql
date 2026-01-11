CREATE TABLE IF NOT EXISTS workspaces (
    id          CHAR(26)     NOT NULL,
    owner_id    CHAR(26)     NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NULL,
    CONSTRAINT pk_workspaces PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS workspace_members (
    id           CHAR(26)     NOT NULL,
    workspace_id CHAR(26)     NOT NULL,
    user_id      CHAR(26)     NOT NULL,
    role         VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NULL,
    CONSTRAINT pk_workspace_members PRIMARY KEY (id)
    );

CREATE INDEX IF NOT EXISTS idx_workspace_members_access ON workspace_members (workspace_id, user_id, deleted_at);

CREATE TABLE IF NOT EXISTS projects (
    id           CHAR(26)     NOT NULL,
    workspace_id CHAR(26)     NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  TEXT         NULL,
    settings     TEXT         NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP    NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS project_members (
    id         CHAR(26)    NOT NULL,
    project_id CHAR(26)    NOT NULL,
    user_id    CHAR(26)    NOT NULL,
    role       VARCHAR(32) NOT NULL,
    joined_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP   NULL,
    CONSTRAINT pk_project_members PRIMARY KEY (id)
    );

CREATE INDEX IF NOT EXISTS idx_project_members_access ON project_members (project_id, user_id, deleted_at);
