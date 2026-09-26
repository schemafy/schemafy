import { useQuery } from '@tanstack/react-query';
import { getSchemasByProjectId } from '../api';
import { erdKeys } from './query-keys';

export const useSchemas = (projectId: string) => {
  const hasProjectId = Boolean(projectId);
  return useQuery({
    queryKey: erdKeys.schemas(projectId),
    queryFn: () => getSchemasByProjectId(projectId),
    enabled: hasProjectId,
  });
};
