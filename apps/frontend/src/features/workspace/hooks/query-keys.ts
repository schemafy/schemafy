export const workspaceKeys = {
  all: ['workspace'] as const,
  lists: () => [...workspaceKeys.all, 'list'] as const,
  list: (page: number, size: number) =>
    [...workspaceKeys.lists(), { page, size }] as const,
  detail: (id: string) => [...workspaceKeys.all, 'detail', id] as const,
  members: (workspaceId: string) =>
    [...workspaceKeys.all, 'members', workspaceId] as const,
  invitations: (workspaceId: string) =>
    [...workspaceKeys.all, 'invitations', workspaceId] as const,
  myInvitations: () => [...workspaceKeys.all, 'myInvitations'] as const,
};