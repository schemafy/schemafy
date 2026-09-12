import { useMutation } from '@tanstack/react-query';
import { undoOperation, redoOperation } from '../api';

export const useUndo = () => {
  return useMutation({
    mutationFn: (opId: string) => undoOperation(opId),
  });
};

export const useRedo = () => {
  return useMutation({
    mutationFn: (opId: string) => redoOperation(opId),
  });
};
