const ROLE_LEVEL: Record<string, number> = {
  ADMIN: 0,
  EDITOR: 1,
  VIEWER: 2,
};

export const getRoleLevel = (role: string) => ROLE_LEVEL[role] ?? Infinity;

export const availableRoles = (userRole: string) => {
  return Object.keys(ROLE_LEVEL).filter(
    (r) => getRoleLevel(r) >= getRoleLevel(userRole),
  );
};
