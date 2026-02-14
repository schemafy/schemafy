import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createSchema, changeSchemaName, deleteSchema } from '../api';
import type { CreateSchemaRequest, ChangeSchemaNameRequest } from '../api';
import { erdKeys } from './query-keys';
import { useErdCache } from './useErdCache';

export const useCreateSchema = (schemaId?: string) => {
  const queryClient = useQueryClient();
  const erdCache = schemaId ? useErdCache(schemaId) : null;

  return useMutation({
    mutationFn: (data: CreateSchemaRequest) => createSchema(data),
    onSuccess: async (response) => {
      if (erdCache && response.affectedTableIds.length > 0) {
        await erdCache.updateAffectedTables(response.affectedTableIds);
      }
      queryClient.invalidateQueries({
        queryKey: erdKeys.all,
      });
    },
  });
};

export const useChangeSchemaName = (schemaId?: string) => {
  const queryClient = useQueryClient();
  const erdCache = schemaId ? useErdCache(schemaId) : null;

  return useMutation({
    mutationFn: ({
      schemaId,
      data,
    }: {
      schemaId: string;
      data: ChangeSchemaNameRequest;
    }) => changeSchemaName(schemaId, data),
    onSuccess: async (response) => {
      if (erdCache && response.affectedTableIds.length > 0) {
        await erdCache.updateAffectedTables(response.affectedTableIds);
      }
      queryClient.invalidateQueries({
        queryKey: erdKeys.all,
      });
    },
  });
};

export const useDeleteSchema = (schemaId?: string) => {
  const queryClient = useQueryClient();
  const erdCache = schemaId ? useErdCache(schemaId) : null;

  return useMutation({
    mutationFn: (schemaId: string) => deleteSchema(schemaId),
    onSuccess: async (response, deletedSchemaId) => {
      queryClient.removeQueries({
        queryKey: erdKeys.schemaSnapshots(deletedSchemaId),
      });
      if (erdCache && response.affectedTableIds.length > 0) {
        await erdCache.updateAffectedTables(response.affectedTableIds);
      }
      queryClient.invalidateQueries({
        queryKey: erdKeys.all,
      });
    },
  });
};
