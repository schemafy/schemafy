import { useState } from 'react';
import { ShareLinkStore } from '@/store';
import type { ShareLinkRole } from '@/lib/api/shareLink/types';
import type { ProjectMember } from '@/lib/api/project/types';

export const useShareLinkStore = () => {
  const store = ShareLinkStore.getInstance();
  const [generatedLink, setGeneratedLink] = useState<string | null>(null);

  const isLoading = store.isLoading('createShareLink');
  const error = store.error;
  const currentShareLink = store.currentShareLink;

  const createShareLink = async (
    workspaceId: string,
    projectId: string,
    role: ShareLinkRole,
    expiresAt?: string,
  ) => {
    const shareLink = await store.createShareLink(workspaceId, projectId, {
      role,
      expiresAt,
    });

    if (shareLink) {
      const url = `${window.location.origin}/join/${shareLink.token}`;
      setGeneratedLink(url);
      return url;
    }
    return null;
  };

  const joinByShareLink = async (
    token: string,
  ): Promise<ProjectMember | null> => {
    return store.joinByShareLink(token);
  };

  const clearShareLink = () => {
    store.clearShareLink();
    setGeneratedLink(null);
  };

  const clearError = () => {
    store.clearError();
  };

  return {
    isLoading,
    error,
    currentShareLink,
    generatedLink,
    createShareLink,
    joinByShareLink,
    clearShareLink,
    clearError,
  };
};
