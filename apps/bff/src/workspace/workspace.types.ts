export type WorkspaceResponse = {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
  projectCount: number;
  currentUserRole: string;
};

export type WorkspaceSummaryResponse = {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
};

export type WorkspaceMemberResponse = {
  workspaceId: string;
  userId: string;
  userName: string;
  userEmail: string;
  role: string;
  joinedAt: string;
};

export type WorkspaceInvitationResponse = {
  id: string;
  workspaceId: string;
  invitedEmail: string;
  invitedRole: string;
  invitedBy: string;
  status: string;
  expiresAt: string;
  resolvedAt?: string;
  createdAt: string;
};

export type WorkspaceInvitationCreateResponse = {
  id: string;
  workspaceId: string;
  invitedEmail: string;
  invitedRole: string;
  invitedBy: string;
  status: string;
  expiresAt: string;
  createdAt: string;
};

export type CreateWorkspaceRequest = {
  name: string;
  description?: string;
};

export type UpdateWorkspaceRequest = {
  name: string;
  description?: string;
};

export type UpdateMemberRoleRequest = {
  role: string;
};

export type CreateWorkspaceInvitationRequest = {
  email: string;
  role: string;
};
