import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import {
  CreateMemoCommentRequest,
  CreateMemoRequest,
  Memo,
  MemoComment,
  UpdateMemoCommentRequest,
  UpdateMemoRequest,
} from './memo.types';

@Injectable()
export class MemoService {
  constructor(private readonly backendClient: BackendClientService) {}

  async createMemo(data: CreateMemoRequest, authHeader: string): Promise<Memo> {
    const response = await this.backendClient.client.post<Memo>(
      '/api/v1.0/memos',
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getMemo(memoId: string, authHeader: string): Promise<Memo> {
    const response = await this.backendClient.client.get<Memo>(
      `/api/v1.0/memos/${memoId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getSchemaMemos(schemaId: string, authHeader: string): Promise<Memo[]> {
    const response = await this.backendClient.client.get<Memo[]>(
      `/api/v1.0/schemas/${schemaId}/memos`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getSchemaMemosWithComments(
    schemaId: string,
    authHeader: string,
  ): Promise<Memo[]> {
    const memosResponse = await this.backendClient.client.get<Memo[]>(
      `/api/v1.0/schemas/${schemaId}/memos`,
      this.backendClient.getAuthConfig(authHeader),
    );

    const memos = memosResponse.data;

    const commentsResults = await Promise.allSettled(
      memos.map((memo) =>
        this.backendClient.client.get<MemoComment[]>(
          `/api/v1.0/memos/${memo.id}/comments`,
          this.backendClient.getAuthConfig(authHeader),
        ),
      ),
    );

    const memosWithComments = memos.map((memo, index) => {
      const result = commentsResults[index];
      const comments = result.status === 'fulfilled' ? result.value.data : [];
      return { ...memo, comments };
    });

    return memosWithComments;
  }

  async updateMemo(
    memoId: string,
    data: UpdateMemoRequest,
    authHeader: string,
  ): Promise<Memo> {
    const response = await this.backendClient.client.put<Memo>(
      `/api/v1.0/memos/${memoId}`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteMemo(memoId: string, authHeader: string): Promise<null> {
    const response = await this.backendClient.client.delete<null>(
      `/api/v1.0/memos/${memoId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async createMemoComment(
    memoId: string,
    data: CreateMemoCommentRequest,
    authHeader: string,
  ): Promise<MemoComment> {
    const response = await this.backendClient.client.post<MemoComment>(
      `/api/v1.0/memos/${memoId}/comments`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getMemoComments(
    memoId: string,
    authHeader: string,
  ): Promise<MemoComment[]> {
    const response = await this.backendClient.client.get<MemoComment[]>(
      `/api/v1.0/memos/${memoId}/comments`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async updateMemoComment(
    memoId: string,
    commentId: string,
    data: UpdateMemoCommentRequest,
    authHeader: string,
  ): Promise<MemoComment> {
    const response = await this.backendClient.client.put<MemoComment>(
      `/api/v1.0/memos/${memoId}/comments/${commentId}`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteMemoComment(
    memoId: string,
    commentId: string,
    authHeader: string,
  ): Promise<null> {
    const response = await this.backendClient.client.delete<null>(
      `/api/v1.0/memos/${memoId}/comments/${commentId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }
}
