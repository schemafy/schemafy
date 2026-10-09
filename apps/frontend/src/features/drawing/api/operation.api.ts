import { apiClient } from '@/lib/api';
import type { MutationResponse } from './types';

export const undoOperation = async (
  opId: string,
): Promise<MutationResponse> => {
  const { data } = await apiClient.post<MutationResponse>(
    `/operations/${opId}/undo`,
  );
  return data;
};

export const redoOperation = async (
  opId: string,
): Promise<MutationResponse> => {
  const { data } = await apiClient.post<MutationResponse>(
    `/operations/${opId}/redo`,
  );
  return data;
};
