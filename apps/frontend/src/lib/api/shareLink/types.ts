export type CreateShareLinkResponse = {
  id: string;
  projectId: string;
  token: string;
  role: ShareLinkRole;
  expiresAt?: string;
  isRevoked: boolean;
  lastAccessedAt: string;
  accessCount: number;
  createdAt: string;
};

export type ShareLinkRole = 'viewer' | 'commenter' | 'editor';

export type CreateShareLinkRequest = {
  role: ShareLinkRole;
  expiresAt?: string;
};

export type GetShareLinkResponse = {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  content: CreateShareLinkResponse[];
};