import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getTableSnapshots } from '../api';
import type { TableSnapshotResponse } from '../api';
import { erdKeys } from './query-keys';

export const useErdCache = (schemaId: string) => {
  const queryClient = useQueryClient();

  const updateAffectedTables = useCallback(
    async (affectedTableIds: string[]) => {
      if (affectedTableIds.length === 0) return;
      try {
        const snapshots = await getTableSnapshots(affectedTableIds);

        const deletedTableIds = affectedTableIds.filter(
          (id) => !(id in snapshots),
        );

        queryClient.setQueryData<Record<string, TableSnapshotResponse>>(
          erdKeys.schemaSnapshots(schemaId),
          (old) => {
            if (!old) return snapshots;
            const updated = { ...old, ...snapshots };
            deletedTableIds.forEach((id) => delete updated[id]);
            return updated;
          },
        );
      } catch {
        queryClient.invalidateQueries({
          queryKey: erdKeys.schemaSnapshots(schemaId),
        });
      }
    },
    [schemaId, queryClient],
  );

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
