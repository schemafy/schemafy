import { Injectable } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import {
  ApiResponse,
  CreateMemoCommentRequest,
  CreateMemoRequest,
  Memo,
  MemoComment,
  UpdateMemoCommentRequest,
  UpdateMemoRequest,
} from './memo.types';

@Injectable()
export class MemoService {
  private readonly backendClient: AxiosInstance;

  constructor() {
    this.backendClient = axios.create({
      baseURL: process.env.BACKEND_URL || 'http://localhost:8080',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: true,
    });
  }

  private getAuthConfig(authHeader: string) {
    return {
      headers: { Authorization: authHeader },
    };
  }

  async createMemo(
    data: CreateMemoRequest,
    authHeader: string,
  ): Promise<ApiResponse<Memo>> {
    const response = await this.backendClient.post<ApiResponse<Memo>>(
      '/api/v1.0/memos',
      data,
      this.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getMemo(
    memoId: string,
    authHeader: string,
  ): Promise<ApiResponse<Memo>> {
    const response = await this.backendClient.get<ApiResponse<Memo>>(
      `/api/v1.0/memos/${memoId}`,
      this.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getSchemaMemos(
    schemaId: string,
    authHeader: string,
  ): Promise<ApiResponse<Memo[]>> {
    const response = await this.backendClient.get<ApiResponse<Memo[]>>(
      `/api/v1.0/schemas/${schemaId}/memos`,
      this.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getSchemaMemosWithComments(
    schemaId: string,
    authHeader: string,
  ): Promise<ApiResponse<Memo[]>> {
    const memosResponse = await this.backendClient.get<ApiResponse<Memo[]>>(
      `/api/v1.0/schemas/${schemaId}/memos`,
      this.getAuthConfig(authHeader),
    );

    if (!memosResponse.data.success || !memosResponse.data.result) {
      return memosResponse.data;
    }

    const memos = memosResponse.data.result;

    const commentsResults = await Promise.allSettled(
      memos.map((memo) =>
        this.backendClient.get<ApiResponse<MemoComment[]>>(
          `/api/v1.0/memos/${memo.id}/comments`,
          this.getAuthConfig(authHeader),
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
    const response = await this.backendClient.put<ApiResponse<Memo>>(
      `/api/v1.0/memos/${memoId}`,
      data,
      this.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteMemo(
    memoId: string,
    authHeader: string,
  ): Promise<ApiResponse<null>> {
    const response = await this.backendClient.delete<ApiResponse<null>>(
      `/api/v1.0/memos/${memoId}`,
      this.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async createMemoComment(
    memoId: string,
    data: CreateMemoCommentRequest,
    authHeader: string,
  ): Promise<ApiResponse<MemoComment>> {
    const response = await this.backendClient.post<ApiResponse<MemoComment>>(
      `/api/v1.0/memos/${memoId}/comments`,
      data,
      this.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getMemoComments(
    memoId: string,
    authHeader: string,
  ): Promise<ApiResponse<MemoComment[]>> {
    const response = await this.backendClient.get<ApiResponse<MemoComment[]>>(
      `/api/v1.0/memos/${memoId}/comments`,
      this.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async updateMemoComment(
    memoId: string,
    commentId: string,
    data: UpdateMemoCommentRequest,
    authHeader: string,
  ): Promise<ApiResponse<MemoComment>> {
    const response = await this.backendClient.put<ApiResponse<MemoComment>>(
      `/api/v1.0/memos/${memoId}/comments/${commentId}`,
      data,
      this.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteMemoComment(
    memoId: string,
    commentId: string,
    authHeader: string,
  ): Promise<ApiResponse<null>> {
    const response = await this.backendClient.delete<ApiResponse<null>>(
      `/api/v1.0/memos/${memoId}/comments/${commentId}`,
      this.getAuthConfig(authHeader),
    );
    return response.data;
  }
}
