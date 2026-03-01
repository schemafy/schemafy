import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createSchema, changeSchemaName, deleteSchema } from '../api';
import type { CreateSchemaRequest, ChangeSchemaNameRequest } from '../api';
import { erdKeys } from './query-keys';

export const useCreateSchema = (projectId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateSchemaRequest) => createSchema(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: erdKeys.schemas(projectId),
      });
    },
  });
};

export const useChangeSchemaName = (projectId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      schemaId,
      data,
    }: {
      schemaId: string;
      data: ChangeSchemaNameRequest;
    }) => changeSchemaName(schemaId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: erdKeys.schemas(projectId),
      });
    },
  });
};

export const useDeleteSchema = (projectId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (schemaId: string) => deleteSchema(schemaId),
    onSuccess: (_, deletedSchemaId) => {
      queryClient.removeQueries({
        queryKey: erdKeys.schemaSnapshots(deletedSchemaId),
      });
      queryClient.invalidateQueries({
        queryKey: erdKeys.schemas(projectId),
      });
    },
  });
};
