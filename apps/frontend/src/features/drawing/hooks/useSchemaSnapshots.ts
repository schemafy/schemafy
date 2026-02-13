import { useSuspenseQuery } from '@tanstack/react-query';
import { getSchemaWithSnapshots } from '../api';
import { erdKeys } from './query-keys';

export const useSchemaSnapshots = (schemaId: string) => {
  return useSuspenseQuery({
    queryKey: erdKeys.schemaSnapshots(schemaId),
    queryFn: () => getSchemaWithSnapshots(schemaId),
    staleTime: Infinity,
  });
};
