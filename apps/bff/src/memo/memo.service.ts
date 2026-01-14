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

  async createMemo(
    data: CreateMemoRequest,
    authHeader: string,
  ): Promise<ApiResponse<Memo>> {
    const response = await this.backendClient.post<ApiResponse<Memo>>(
      '/api/v1.0/memos',
      data,
      {
        headers: authHeader ? { Authorization: authHeader } : {},
      },
    );
    return response.data;
  }

  async getMemo(
    memoId: string,
    authHeader: string,
  ): Promise<ApiResponse<Memo>> {
    const response = await this.backendClient.get<ApiResponse<Memo>>(
      `/api/v1.0/memos/${memoId}`,
      {
        headers: authHeader ? { Authorization: authHeader } : {},
      },
    );
    return response.data;
  }

  async getSchemaMemos(
    schemaId: string,
    authHeader: string,
  ): Promise<ApiResponse<Memo[]>> {
    const response = await this.backendClient.get<ApiResponse<Memo[]>>(
      `/api/v1.0/schemas/${schemaId}/memos`,
      {
        headers: authHeader ? { Authorization: authHeader } : {},
      },
    );
    return response.data;
  }

  async updateMemo(
    memoId: string,
    data: UpdateMemoRequest,
    authHeader: string,
  ): Promise<ApiResponse<Memo>> {
    const response = await this.backendClient.put<ApiResponse<Memo>>(
      `/api/v1.0/memos/${memoId}`,
      data,
      {
        headers: authHeader ? { Authorization: authHeader } : {},
      },
    );
    return response.data;
  }

  async deleteMemo(
    memoId: string,
    authHeader: string,
  ): Promise<ApiResponse<null>> {
    const response = await this.backendClient.delete<ApiResponse<null>>(
      `/api/v1.0/memos/${memoId}`,
      {
        headers: authHeader ? { Authorization: authHeader } : {},
      },
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
      {
        headers: authHeader ? { Authorization: authHeader } : {},
      },
    );
    return response.data;
  }

  async getMemoComments(
    memoId: string,
    authHeader: string,
  ): Promise<ApiResponse<MemoComment[]>> {
    const response = await this.backendClient.get<ApiResponse<MemoComment[]>>(
      `/api/v1.0/memos/${memoId}/comments`,
      {
        headers: authHeader ? { Authorization: authHeader } : {},
      },
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
      {
        headers: authHeader ? { Authorization: authHeader } : {},
      },
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
      {
        headers: authHeader ? { Authorization: authHeader } : {},
      },
    );
    return response.data;
  }
}
