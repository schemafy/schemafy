import { useQuery } from '@tanstack/react-query';
import { getSchemaWithSnapshots } from '../api';
import { erdKeys } from './query-keys';

export const useSchemaSnapshots = (schemaId: string) => {
  return useQuery({
    queryKey: erdKeys.schemaSnapshots(schemaId),
    queryFn: () => getSchemaWithSnapshots(schemaId),
    enabled: !!schemaId,
    staleTime: Infinity,
  });
};
