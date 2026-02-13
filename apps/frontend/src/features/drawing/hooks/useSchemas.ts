import { useQuery } from '@tanstack/react-query';
import { getSchemasByProjectId } from '../api';
import { erdKeys } from './query-keys';

export const useSchemas = (projectId: string) => {
  return useQuery({
    queryKey: erdKeys.schemas(projectId),
    queryFn: () => getSchemasByProjectId(projectId),
    enabled: !!projectId,
  });
};
