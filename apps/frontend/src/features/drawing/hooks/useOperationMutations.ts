import { useMutation } from '@tanstack/react-query';
import { undoOperation, redoOperation } from '../api';
import { useErdCache } from './useErdCache';

export const useUndo = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: async (opId: string) => {
      const result = await undoOperation(opId);
      await syncAffectedTables(result);
      return result;
    },
  });
};

export const useRedo = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: async (opId: string) => {
      const result = await redoOperation(opId);
      await syncAffectedTables(result);
      return result;
    },
  });
};
