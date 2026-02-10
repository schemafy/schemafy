import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import { ApiResponse } from '../common/types/api-response.types';
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

  async createMemo(
    data: CreateMemoRequest,
    authHeader: string,
  ): Promise<ApiResponse<Memo>> {
    const response = await this.backendClient.client.post<ApiResponse<Memo>>(
      '/api/v1.0/memos',
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getMemo(
    memoId: string,
    authHeader: string,
  ): Promise<ApiResponse<Memo>> {
    const response = await this.backendClient.client.get<ApiResponse<Memo>>(
      `/api/v1.0/memos/${memoId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getSchemaMemos(
    schemaId: string,
    authHeader: string,
  ): Promise<ApiResponse<Memo[]>> {
    const response = await this.backendClient.client.get<ApiResponse<Memo[]>>(
      `/api/v1.0/schemas/${schemaId}/memos`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getSchemaMemosWithComments(
    schemaId: string,
    authHeader: string,
  ): Promise<ApiResponse<Memo[]>> {
    const memosResponse = await this.backendClient.client.get<ApiResponse<Memo[]>>(
      `/api/v1.0/schemas/${schemaId}/memos`,
      this.backendClient.getAuthConfig(authHeader),
    );

    if (!memosResponse.data.success || !memosResponse.data.result) {
      return memosResponse.data;
    }

    const memos = memosResponse.data.result;

    const commentsResults = await Promise.allSettled(
      memos.map((memo) =>
        this.backendClient.client.get<ApiResponse<MemoComment[]>>(
          `/api/v1.0/memos/${memo.id}/comments`,
          this.backendClient.getAuthConfig(authHeader),
        ),
      ),
    );

    const memosWithComments = memos.map((memo, index) => {
      const result = commentsResults[index];
      const comments =
        result.status === 'fulfilled' &&
        result.value.data.success &&
        result.value.data.result
          ? result.value.data.result
          : [];
      return { ...memo, comments };
    });

    return {
      success: true,
      result: memosWithComments,
      error: null,
    };
  }

  async updateMemo(
    memoId: string,
    data: UpdateMemoRequest,
    authHeader: string,
  ): Promise<ApiResponse<Memo>> {
    const response = await this.backendClient.client.put<ApiResponse<Memo>>(
      `/api/v1.0/memos/${memoId}`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteMemo(
    memoId: string,
    authHeader: string,
  ): Promise<ApiResponse<null>> {
    const response = await this.backendClient.client.delete<ApiResponse<null>>(
      `/api/v1.0/memos/${memoId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async createMemoComment(
    memoId: string,
    data: CreateMemoCommentRequest,
    authHeader: string,
  ): Promise<ApiResponse<MemoComment>> {
    const response = await this.backendClient.client.post<ApiResponse<MemoComment>>(
      `/api/v1.0/memos/${memoId}/comments`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getMemoComments(
    memoId: string,
    authHeader: string,
  ): Promise<ApiResponse<MemoComment[]>> {
    const response = await this.backendClient.client.get<ApiResponse<MemoComment[]>>(
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
  ): Promise<ApiResponse<MemoComment>> {
    const response = await this.backendClient.client.put<ApiResponse<MemoComment>>(
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
  ): Promise<ApiResponse<null>> {
    const response = await this.backendClient.client.delete<ApiResponse<null>>(
      `/api/v1.0/memos/${memoId}/comments/${commentId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }
}
