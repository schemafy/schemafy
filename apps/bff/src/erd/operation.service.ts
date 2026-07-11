import { Injectable } from '@nestjs/common';
import {
  BackendClientService,
  type CollaborationRequestHeaders,
} from '../common/backend-client/backend-client.service';
import type { MutationResponse } from './erd.types';

@Injectable()
export class OperationService {
  constructor(private readonly backendClient: BackendClientService) {}

  async undo(
    opId: string,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse> {
    const { data } =
      await this.backendClient.client.post<MutationResponse>(
        `/api/v1.0/operations/${opId}/undo`,
        {},
        this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
      );
    return data;
  }

  async redo(
    opId: string,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse> {
    const { data } =
      await this.backendClient.client.post<MutationResponse>(
        `/api/v1.0/operations/${opId}/redo`,
        {},
        this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
      );
    return data;
  }
}
