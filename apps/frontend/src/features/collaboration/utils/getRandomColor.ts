import { COLORS } from '@/features/collaboration/utils/constants.ts';

export const hashId = (id: string): number => {
  let hash = 0;
  for (let i = 0; i < id.length; i++) {
    hash = id.charCodeAt(i) + ((hash << 5) - hash);
    hash |= 0;
  }
  return Math.abs(hash);
};

export const getRandomColor = (id: string): string => {
  return COLORS[hashId(id) % COLORS.length];
};

export const getInitials = (name: string): string => {
  return Array.from(name.trim())[0]?.toUpperCase() ?? '?';
};
