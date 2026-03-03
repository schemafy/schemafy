import type { QueryClient } from '@tanstack/react-query';
import type { TableSnapshotResponse } from '../api';
import { getTableSnapshots } from '../api';
import { erdKeys } from '../hooks/query-keys';

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
    await queryClient.invalidateQueries({
      queryKey: erdKeys.schemaSnapshots(schemaId),
    });
  }
};

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
