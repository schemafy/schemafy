export const projectKeys = {
  all: ['project'] as const,
  lists: (workspaceId: string) =>
    [...projectKeys.all, 'list', workspaceId] as const,
  list: (workspaceId: string, page: number, size: number) =>
    [...projectKeys.lists(workspaceId), { page, size }] as const,
  detail: (projectId: string) =>
    [...projectKeys.all, 'detail', projectId] as const,
  membersAll: (projectId: string) =>
    [...projectKeys.all, 'members', projectId] as const,
  members: (projectId: string, page: number, size: number) =>
    [...projectKeys.all, 'members', projectId, { page, size }] as const,
  invitationsAll: (projectId: string) =>
    [...projectKeys.all, 'invitations', projectId] as const,
  invitations: (projectId: string, page: number, size: number) =>
    [...projectKeys.all, 'invitations', projectId, { page, size }] as const,
  myInvitationsAll: () => [...projectKeys.all, 'myInvitations'] as const,
  myInvitations: (page: number, size: number) =>
    [...projectKeys.all, 'myInvitations', { page, size }] as const,
};
