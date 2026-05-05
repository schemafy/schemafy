export type ProjectResponse = {
  id: string;
  workspaceId: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
  currentUserRole: string;
};

export type ProjectSummaryResponse = {
  id: string;
  workspaceId: string;
  name: string;
  description?: string;
  myRole: string;
  createdAt: string;
  updatedAt: string;
};

export type ProjectMemberResponse = {
  projectId: string;
  userId: string;
  userName: string;
  userEmail: string;
  role: string;
  joinedAt: string;
};

export type ProjectInvitationResponse = {
  id: string;
  workspaceId: string;
  projectId: string;
  invitedEmail: string;
  invitedRole: string;
  invitedBy: string;
  status: string;
  expiresAt: string;
  resolvedAt?: string;
  createdAt: string;
};

export type ProjectInvitationCreateResponse = {
  id: string;
  projectId: string;
  workspaceId: string;
  invitedEmail: string;
  invitedRole: string;
  invitedBy: string;
  status: string;
  expiresAt: string;
  createdAt: string;
};

export type CreateProjectRequest = {
  name: string;
  description?: string;
};

export type UpdateProjectRequest = {
  name: string;
  description?: string;
};

export type CreateProjectInvitationRequest = {
  email: string;
  role: string;
};

export type UpdateProjectMemberRoleRequest = {
  role: string;
};
