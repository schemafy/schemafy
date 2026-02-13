import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createSchema, changeSchemaName, deleteSchema } from '../api';
import type { CreateSchemaRequest, ChangeSchemaNameRequest } from '../api';
import { erdKeys } from './query-keys';

export const useCreateSchema = () => {
  return useMutation({
    mutationFn: (data: CreateSchemaRequest) => createSchema(data),
  });
};

export const useChangeSchemaName = () => {
  return useMutation({
    mutationFn: ({
      schemaId,
      data,
    }: {
      schemaId: string;
      data: ChangeSchemaNameRequest;
    }) => changeSchemaName(schemaId, data),
  });
};

export const useDeleteSchema = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (schemaId: string) => deleteSchema(schemaId),
    onSuccess: (_, schemaId) => {
      queryClient.removeQueries({
        queryKey: erdKeys.schemaSnapshots(schemaId),
      });
    },
  });
};
