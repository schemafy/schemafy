import { makeAutoObservable } from 'mobx';
import { createShareLink } from '@/lib/api/shareLink/api';
import { joinProjectByShareLink } from '@/lib/api/project/api';
import type {
  CreateShareLinkRequest,
  CreateShareLinkResponse,
} from '@/lib/api/shareLink/types';
import type { ProjectMember } from '@/lib/api/project/types';
import { handleAsync, type AsyncHandlerContext } from './helpers';

export class ShareLinkStore implements AsyncHandlerContext {
  private static instance: ShareLinkStore;

  currentShareLink: CreateShareLinkResponse | null = null;

  _loadingStates: Record<string, boolean> = {};
  error: string | null = null;

  private constructor() {
    makeAutoObservable(this);
  }

  static getInstance(): ShareLinkStore {
    if (!ShareLinkStore.instance) {
      ShareLinkStore.instance = new ShareLinkStore();
    }
    return ShareLinkStore.instance;
  }

  isLoading(operation: string): boolean {
    return !!this._loadingStates[operation];
  }

  async createShareLink(
    workspaceId: string,
    projectId: string,
    data: CreateShareLinkRequest,
  ): Promise<CreateShareLinkResponse | null> {
    const { data: shareLink } = await handleAsync(
      this,
      'createShareLink',
      () => createShareLink(workspaceId, projectId, data),
      (result) => {
        this.currentShareLink = result;
      },
      'Failed to create share link',
    );
    return shareLink;
  }

  async joinByShareLink(token: string): Promise<ProjectMember | null> {
    const { data: member } = await handleAsync(
      this,
      'joinByShareLink',
      () => joinProjectByShareLink({ token }),
      () => {},
      'Failed to join project',
    );
    return member;
  }

  getShareLinkUrl(): string | null {
    if (!this.currentShareLink) return null;
    return `${window.location.origin}/join/${this.currentShareLink.token}`;
  }

  clearShareLink() {
    this.currentShareLink = null;
  }

  clearError() {
    this.error = null;
  }
}
