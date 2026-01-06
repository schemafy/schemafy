type ProjectSummary = {
  id: string;
  workspaceId: string;
  name: string;
  description: string;
  myRole: ProjectRole;
  memberCount: number;
  createdAt: string;
  updatedAt: string;
};

type ProjectSettings = {
  theme?: string;
  language?: string;
  defaultView?: string;
};

type ProjectRole = 'OWNER' | 'ADMIN' | 'EDITOR' | 'COMMENTER' | 'VIEWER';

export type Project = {
  id: string;
  workspaceId: string;
  name: string;
  description: string;
  settings?: ProjectSettings;
  createdAt: string;
  updatedAt: string;
};

export type ProjectMember = {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  role: ProjectRole;
  joinedAt: string;
};

export type ProjectsResponse = {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  content: ProjectSummary[];
}

export type ProjectsMembersResponse = {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  content: ProjectMember[];
}

export type ProjectRequest = {
  name: string;
  description: string;
  settings?: ProjectSettings;
};

export type JoinProjectByShareLinkRequest = {
  token: string;
};

export type UpdateProjectMemberRoleRequest = {
  role: ProjectRole;
};
