CREATE TABLE IF NOT EXISTS users (
    id          CHAR(26)     NOT NULL,
    email       VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS workspaces (
    id          CHAR(26)     NOT NULL,
    owner_id    CHAR(26)     NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NULL,
    settings    TEXT         NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NULL,
    CONSTRAINT pk_workspaces PRIMARY KEY (id),
    INDEX idx_workspaces_owner_id (owner_id),
    INDEX idx_workspaces_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS workspace_members (
    id           CHAR(26)     NOT NULL,
    workspace_id CHAR(26)     NOT NULL,
    user_id      CHAR(26)     NOT NULL,
    role         VARCHAR(32)  NOT NULL,
    joined_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP    NULL,
    CONSTRAINT pk_workspace_members PRIMARY KEY (id),
    CONSTRAINT uq_workspace_members_workspace_user UNIQUE (workspace_id, user_id),
    INDEX idx_workspace_members_user_id (user_id),
    INDEX idx_workspace_members_workspace_id (workspace_id),
    INDEX idx_workspace_members_user_deleted (user_id, deleted_at),
    INDEX idx_workspace_members_deleted_at (deleted_at),
    CONSTRAINT ck_workspace_members_role CHECK (role IN ('admin','member'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS projects (
    id           CHAR(26)     NOT NULL,
    workspace_id CHAR(26)     NOT NULL,
    owner_id     CHAR(26)     NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  TEXT         NULL,
    settings     TEXT         NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP    NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id),
    INDEX idx_projects_workspace_id (workspace_id),
    INDEX idx_projects_owner_id (owner_id),
    INDEX idx_projects_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_members (
    id         CHAR(26)    NOT NULL,
    project_id CHAR(26)    NOT NULL,
    user_id    CHAR(26)    NOT NULL,
    role       VARCHAR(32) NOT NULL,
    joined_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP   NULL,
    CONSTRAINT pk_project_members PRIMARY KEY (id),
    CONSTRAINT uq_project_members_project_user UNIQUE (project_id, user_id),
    INDEX idx_project_members_user_id (user_id),
    INDEX idx_project_members_project_id (project_id),
    INDEX idx_project_members_user_deleted (user_id, deleted_at),
    INDEX idx_project_members_deleted_at (deleted_at),
    CONSTRAINT ck_project_members_role CHECK (role IN ('owner','admin','editor','viewer'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;