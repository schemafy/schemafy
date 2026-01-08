import { runInAction } from 'mobx';
import type { ApiResponse } from '@/lib/api/types';

export type AsyncHandlerContext = {
  _loadingStates: Record<string, boolean>;
  error: string | null;
};

export type AsyncHandlerResult<T> = {
  success: boolean;
  data: T | null;
};

export async function handleAsync<T>(
  context: AsyncHandlerContext,
  operation: string,
  apiCall: () => Promise<ApiResponse<T>>,
  onSuccess: (result: T) => void,
  defaultErrorMessage: string,
): Promise<AsyncHandlerResult<T>> {
  context._loadingStates[operation] = true;
  context.error = null;

  try {
    const res = await apiCall();
    if (!res.success) {
      runInAction(() => {
        context.error = res.error?.message ?? defaultErrorMessage;
        context._loadingStates[operation] = false;
      });
      return { success: false, data: null };
    }

    runInAction(() => {
      onSuccess(res.result as T);
      context._loadingStates[operation] = false;
    });

    return { success: true, data: res.result as T };
  } catch (e) {
    runInAction(() => {
      context.error = e instanceof Error ? e.message : defaultErrorMessage;
      context._loadingStates[operation] = false;
    });
    return { success: false, data: null };
  }
}
