type WorkspaceSummary = {
  id: string;
  name: string;
  description: string;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
  memberCount: number;
};

type WorkspaceMember = {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  role: WorkspaceRole;
  joinedAt: string;
};

type WorkspaceSettings = {
  theme?: string;
  language?: string;
};

type WorkspaceRole = 'ADMIN' | 'MEMBER';

export type Workspace = {
  id: string;
  name: string;
  description: string;
  ownerId: string;
  settings?: WorkspaceSettings;
  createdAt: string;
  updatedAt: string;
};

export type WorkspaceMemberResponse = {
  content: WorkspaceMember[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type WorkspaceRequest = {
  name: string;
  description: string;
  settings?: WorkspaceSettings;
};

export type WorkspacesResponse = {
  content: WorkspaceSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
