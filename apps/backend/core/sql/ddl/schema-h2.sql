CREATE TABLE IF NOT EXISTS users (
    id        CHAR(26)     NOT NULL,
    email     VARCHAR(255) NOT NULL,
    name      VARCHAR(255) NOT NULL,
    password  VARCHAR(255) NOT NULL,
    status    VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP  NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ux_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED'))
);

CREATE TABLE IF NOT EXISTS workspaces (
    id          CHAR(26)     NOT NULL,
    owner_id    CHAR(26)     NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description CLOB         NULL,
    settings    CLOB         NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NULL,
    CONSTRAINT pk_workspaces PRIMARY KEY (id)
);
CREATE INDEX idx_workspaces_owner_id ON workspaces(owner_id);
CREATE INDEX idx_workspaces_deleted_at ON workspaces(deleted_at);

CREATE TABLE IF NOT EXISTS workspace_members (
    id           CHAR(26)     NOT NULL,
    workspace_id CHAR(26)     NOT NULL,
    user_id      CHAR(26)     NOT NULL,
    role         VARCHAR(32)  NOT NULL,
    joined_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP    NULL,
    CONSTRAINT pk_workspace_members PRIMARY KEY (id),
    CONSTRAINT uq_workspace_members_workspace_user UNIQUE (workspace_id, user_id),
    CONSTRAINT ck_workspace_members_role CHECK (role IN ('admin','member'))
);
CREATE INDEX idx_workspace_members_user_id ON workspace_members(user_id);
CREATE INDEX idx_workspace_members_workspace_id ON workspace_members(workspace_id);
CREATE INDEX idx_workspace_members_deleted_at ON workspace_members(deleted_at);

CREATE TABLE IF NOT EXISTS projects (
    id           CHAR(26)     NOT NULL,
    workspace_id CHAR(26)     NOT NULL,
    owner_id     CHAR(26)     NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  CLOB         NULL,
    settings     CLOB         NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP    NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id)
);
CREATE INDEX idx_projects_workspace_id ON projects(workspace_id);
CREATE INDEX idx_projects_owner_id ON projects(owner_id);
CREATE INDEX idx_projects_deleted_at ON projects(deleted_at);

CREATE TABLE IF NOT EXISTS project_members (
    id         CHAR(26)    NOT NULL,
    project_id CHAR(26)    NOT NULL,
    user_id    CHAR(26)    NOT NULL,
    role       VARCHAR(32) NOT NULL,
    joined_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP   NULL,
    CONSTRAINT pk_project_members PRIMARY KEY (id),
    CONSTRAINT uq_project_members_project_user UNIQUE (project_id, user_id),
    CONSTRAINT ck_project_members_role CHECK (role IN ('owner','admin','editor','viewer'))
);
CREATE INDEX idx_project_members_user_id ON project_members(user_id);
CREATE INDEX idx_project_members_project_id ON project_members(project_id);
CREATE INDEX idx_project_members_deleted_at ON project_members(deleted_at);