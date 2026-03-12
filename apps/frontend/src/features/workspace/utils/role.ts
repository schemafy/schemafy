export const ROLES = ['ADMIN', 'MEMBER'] as const;
type WorkspaceRole = (typeof ROLES)[number];

export const getRoleLevel = (role: string) => ROLES.indexOf(role as WorkspaceRole);

export const availableRoles = (userRole: string) => {
  return ROLES.filter(
    (r) => getRoleLevel(r) >= getRoleLevel(userRole),
  );
}