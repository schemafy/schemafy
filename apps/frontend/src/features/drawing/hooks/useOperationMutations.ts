import { useMutation } from '@tanstack/react-query';
import { undoOperation, redoOperation } from '../api';
import { useErdCache } from './useErdCache';

export const useUndo = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);

  return useMutation({
    mutationFn: (opId: string) => undoOperation(opId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useRedo = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);

  return useMutation({
    mutationFn: (opId: string) => redoOperation(opId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};
