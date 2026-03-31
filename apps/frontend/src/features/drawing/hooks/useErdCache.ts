import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getTableSnapshots } from '../api';
import type { SchemaSnapshotsResponse } from '../api';
import { erdKeys } from './query-keys';
import { collaborationStore } from '@/store/collaboration.store';

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

        queryClient.setQueryData<SchemaSnapshotsResponse>(
          erdKeys.schemaSnapshots(schemaId),
          (old) => {
            const knownRevision =
              collaborationStore.getSchemaRevision(schemaId);

            if (!old) {
              return {
                currentRevision: knownRevision ?? 0,
                snapshots,
              };
            }

            const updated = { ...old.snapshots, ...snapshots };
            deletedTableIds.forEach((id) => delete updated[id]);

            return {
              currentRevision:
                knownRevision === null
                  ? old.currentRevision
                  : Math.max(old.currentRevision, knownRevision),
              snapshots: updated,
            };
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
    queryClient.setQueryData<SchemaSnapshotsResponse>(
      erdKeys.schemaSnapshots(schemaId),
      (old) => {
        if (!old) return old;
        const { [removedTableId]: _, ...rest } = old.snapshots;

        return {
          ...old,
          snapshots: rest,
        };
      },
    );
    await updateAffectedTables(affectedTableIds);
  };

  return { updateAffectedTables, removeAndUpdate };
};
