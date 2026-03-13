export const workspaceKeys = {
  all: ['workspace'] as const,
  lists: () => [...workspaceKeys.all, 'list'] as const,
  list: (page: number, size: number) =>
    [...workspaceKeys.lists(), { page, size }] as const,
  detail: (id: string) => [...workspaceKeys.all, 'detail', id] as const,
  membersAll: (workspaceId: string) =>
    [...workspaceKeys.all, 'members', workspaceId] as const,
  members: (workspaceId: string, page: number, size: number) =>
    [...workspaceKeys.all, 'members', workspaceId, { page, size }] as const,
  invitationsAll: (workspaceId: string) =>
    [...workspaceKeys.all, 'invitations', workspaceId] as const,
  invitations: (workspaceId: string, page: number, size: number) =>
    [...workspaceKeys.all, 'invitations', workspaceId, { page, size }] as const,
  myInvitationsAll: () => [...workspaceKeys.all, 'myInvitations'] as const,
  myInvitations: (page: number, size: number) =>
    [...workspaceKeys.all, 'myInvitations', { page, size }] as const,
};
