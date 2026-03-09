import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { collaborationStore } from '@/store/collaboration.store';
import { useErdCache } from './useErdCache';
import { erdKeys } from './query-keys';

const DEBOUNCE_MS = 300;

export const useErdMutationSync = (schemaId: string, projectId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const queryClient = useQueryClient();
  const pendingTableIds = useRef<Set<string>>(new Set());
  const pendingSchemaChange = useRef(false);
  const timerRef = useRef<number | null>(null);

  useEffect(() => {
    const flush = () => {
      const tableIds = [...pendingTableIds.current];
      const hasSchemaChange = pendingSchemaChange.current;

      pendingTableIds.current.clear();
      pendingSchemaChange.current = false;
      timerRef.current = null;

      if (hasSchemaChange) {
        queryClient.invalidateQueries({
          queryKey: erdKeys.schemas(projectId),
        });
        queryClient.invalidateQueries({
          queryKey: erdKeys.schemaSnapshots(schemaId),
        });
      } else if (tableIds.length > 0) {
        updateAffectedTables(tableIds);
      }
    };

    const scheduleFlush = () => {
      if (timerRef.current !== null) {
        clearTimeout(timerRef.current);
      }
      timerRef.current = window.setTimeout(flush, DEBOUNCE_MS);
    };

    const unsubscribe = collaborationStore.onErdMutated((event) => {
      if (event.schemaId !== schemaId) return;

      if (event.affectedTableIds.length > 0) {
        event.affectedTableIds.forEach((id) => pendingTableIds.current.add(id));
      } else {
        pendingSchemaChange.current = true;
      }

      scheduleFlush();
    });

    return () => {
      unsubscribe();
      if (timerRef.current !== null) {
        clearTimeout(timerRef.current);
      }
    };
  }, [schemaId, projectId, updateAffectedTables, queryClient]);
};
