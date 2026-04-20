import { useLayoutEffect, useRef } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { getSchemaWithSnapshots } from '../api';
import type { SchemaSnapshotsResponse } from '../api';
import { erdKeys } from './query-keys';
import { collaborationStore } from '@/store/collaboration.store';
import { previewStore } from '@/store/preview.store';

const applyPreviewOverlay = (
  schemaId: string,
  base: SchemaSnapshotsResponse,
): SchemaSnapshotsResponse => {
  const entries = [...previewStore.previews.values()].filter(
    (e) => e.schemaId === schemaId,
  );

  if (entries.length === 0) return base;

  const snapshots = { ...base.snapshots };

  for (const entry of entries) {
    if (entry.kind === 'TABLE') {
      const tableId = entry.snapshot.table.id;
      snapshots[tableId] = entry.snapshot;
    } else if (entry.kind === 'RELATIONSHIP') {
      const fkSnapshot = snapshots[entry.fkTableId];
      if (fkSnapshot) {
        snapshots[entry.fkTableId] = {
          ...fkSnapshot,
          relationships: [...fkSnapshot.relationships, entry.snapshot],
        };
      }
    }
  }

  return { ...base, snapshots };
};

export const useSchemaSnapshots = (schemaId: string) => {
  const prevSchemaIdRef = useRef(schemaId);

  const query = useSuspenseQuery({
    queryKey: erdKeys.schemaSnapshots(schemaId),
    queryFn: () => getSchemaWithSnapshots(schemaId),
    staleTime: Infinity,
  });

  useLayoutEffect(() => {
    collaborationStore.setSchemaRevision(schemaId, query.data.currentRevision);
  }, [schemaId, query.data.currentRevision]);

  useLayoutEffect(() => {
    const prev = prevSchemaIdRef.current;
    if (prev !== schemaId) {
      previewStore.clearBySchema(prev);
      prevSchemaIdRef.current = schemaId;
    }
  }, [schemaId]);

  const merged = applyPreviewOverlay(schemaId, query.data);

  return { ...query, data: merged };
};
