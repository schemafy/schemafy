import { useQueryClient } from '@tanstack/react-query';
import { getTableSnapshots } from '../api';
import type { TableSnapshotResponse } from '../api';
import { erdKeys } from './query-keys';

export const useErdCache = (schemaId: string) => {
  const queryClient = useQueryClient();

  const updateAffectedTables = async (affectedTableIds: string[]) => {
    if (affectedTableIds.length === 0) return;
    const snapshots = await getTableSnapshots(affectedTableIds);
    queryClient.setQueryData<Record<string, TableSnapshotResponse>>(
      erdKeys.schemaSnapshots(schemaId),
      (old) => (old ? { ...old, ...snapshots } : snapshots),
    );
  };

  const removeAndUpdate = async (
    removedTableId: string,
    affectedTableIds: string[],
  ) => {
    queryClient.setQueryData<Record<string, TableSnapshotResponse>>(
      erdKeys.schemaSnapshots(schemaId),
      (old) => {
        if (!old) return old;
        const { [removedTableId]: _, ...rest } = old;
        return rest;
      },
    );
    await updateAffectedTables(affectedTableIds);
  };

  return { updateAffectedTables, removeAndUpdate };
};
