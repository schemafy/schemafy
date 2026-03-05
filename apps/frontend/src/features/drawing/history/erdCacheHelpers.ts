import type { QueryClient } from '@tanstack/react-query';
import type { TableSnapshotResponse } from '../api';
import { getTableSnapshots } from '../api';
import { erdKeys } from '../hooks/query-keys';
import type { ErdCommand } from './ErdCommand';
import { toast } from "sonner";

export const updateAffectedTablesInCache = async (
  queryClient: QueryClient,
  schemaId: string,
  affectedTableIds: string[],
): Promise<void> => {
  if (affectedTableIds.length === 0) return;

  try {
    const snapshots = await getTableSnapshots(affectedTableIds);
    queryClient.setQueryData<Record<string, TableSnapshotResponse>>(
      erdKeys.schemaSnapshots(schemaId),
      (old) => (old ? {...old, ...snapshots} : snapshots),
    );
  } catch {
    void queryClient.invalidateQueries({
      queryKey: erdKeys.schemaSnapshots(schemaId),
    });

    toast.error("데이터 동기화 중 오류가 발생했습니다");
  }
};

export abstract class BaseErdCommand implements ErdCommand {
  constructor(
    protected readonly schemaId: string,
    protected readonly queryClient: QueryClient,
  ) {
  }

  abstract undo(): Promise<void>;

  abstract redo(): Promise<void>;

  protected async updateCache(affectedTableIds: string[]): Promise<void> {
    await updateAffectedTablesInCache(
      this.queryClient,
      this.schemaId,
      affectedTableIds,
    );
  }
}

export const removeTableFromCache = (
  queryClient: QueryClient,
  schemaId: string,
  tableId: string,
): void => {
  queryClient.setQueryData<Record<string, TableSnapshotResponse>>(
    erdKeys.schemaSnapshots(schemaId),
    (old) => {
      if (!old) return old;
      const {[tableId]: _, ...rest} = old;
      return rest;
    },
  );
};
