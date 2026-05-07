import { useLayoutEffect } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { getSchemaWithSnapshots } from '../api';
import { erdKeys } from './query-keys';
import { collaborationStore } from '@/store/collaboration.store';

export const useSchemaSnapshots = (schemaId: string) => {
  const query = useSuspenseQuery({
    queryKey: erdKeys.schemaSnapshots(schemaId),
    queryFn: () => getSchemaWithSnapshots(schemaId),
    staleTime: Infinity,
  });

  useLayoutEffect(() => {
    collaborationStore.setSchemaRevision(schemaId, query.data.currentRevision);
  }, [schemaId, query.data.currentRevision]);

  return query;
};
