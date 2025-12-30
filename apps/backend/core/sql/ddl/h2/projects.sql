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
CREATE INDEX IF NOT EXISTS idx_workspaces_owner_deleted ON workspaces(owner_id, deleted_at);

CREATE TABLE IF NOT EXISTS workspace_members (
    id           CHAR(26)     NOT NULL,
    workspace_id CHAR(26)     NOT NULL,
    user_id      CHAR(26)     NOT NULL,
    role         VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NULL,
    CONSTRAINT pk_workspace_members PRIMARY KEY (id),
    CONSTRAINT uq_workspace_members_workspace_user UNIQUE (workspace_id, user_id),
    CONSTRAINT ck_workspace_members_role CHECK (role IN ('admin','member'))
    );
CREATE INDEX IF NOT EXISTS idx_workspace_members_user_deleted ON workspace_members(user_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_workspace_members_workspace_deleted ON workspace_members(workspace_id, deleted_at);

CREATE TABLE IF NOT EXISTS projects (
    id           CHAR(26)     NOT NULL,
    workspace_id CHAR(26)     NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  CLOB         NULL,
    settings     CLOB         NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP    NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id)
    );
CREATE INDEX IF NOT EXISTS idx_projects_workspace_deleted ON projects(workspace_id, deleted_at);

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
    CONSTRAINT ck_project_members_role CHECK (role IN ('owner','admin','editor','viewer', 'commenter'))
    );

CREATE INDEX IF NOT EXISTS idx_project_members_user_deleted ON project_members(user_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_project_members_project_deleted ON project_members(project_id, deleted_at);
