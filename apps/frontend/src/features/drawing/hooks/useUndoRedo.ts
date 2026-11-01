import { useCallback } from 'react';
import { useIsMutating } from '@tanstack/react-query';
import { toast } from 'sonner';
import { useSelectedSchema } from '../contexts';
import { useUndo, useRedo } from './useOperationMutations';
import { operationHistoryStore } from '@/store/operation-history.store';
import { isKnownUndoRedoError } from '../constants/operationError';

export const useUndoRedo = () => {
  const { selectedSchemaId } = useSelectedSchema();
  const { mutate: undo } = useUndo(selectedSchemaId);
  const { mutate: redo } = useRedo(selectedSchemaId);

  const isMutating = useIsMutating() > 0;
  const canUndo =
    operationHistoryStore.getUndoableOpIds(selectedSchemaId).length > 0 &&
    !isMutating;
  const canRedo =
    operationHistoryStore.getRedoOpIds(selectedSchemaId).length > 0 &&
    !isMutating;

  const handleUndo = useCallback(() => {
    if (isMutating) return;

    const latest =
      operationHistoryStore.getLatestUndoableOperation(selectedSchemaId);
    if (!latest?.opId) return;

    const opId = latest.opId;

    undo(opId, {
      onSuccess: () => {
        operationHistoryStore.removeUndoableOpId(selectedSchemaId, opId);
        operationHistoryStore.pushRedoOpId(selectedSchemaId, opId);
      },
      onError: (error) => {
        if (isKnownUndoRedoError(error)) {
          operationHistoryStore.clearSchemaHistory(selectedSchemaId);
        }
      },
    });
  }, [selectedSchemaId, undo, isMutating]);

  const handleRedo = useCallback(() => {
    if (isMutating) return;

    const opId = operationHistoryStore.popRedoOpId(selectedSchemaId);
    if (!opId) return;

    redo(opId, {
      onSuccess: () => {
        operationHistoryStore.addUndoableOpId(selectedSchemaId, opId);
      },
      onError: (error) => {
        toast.error('Undo or redo failed. Please try again.');
        if (isKnownUndoRedoError(error)) {
          operationHistoryStore.clearSchemaHistory(selectedSchemaId);
        } else {
          operationHistoryStore.pushRedoOpId(selectedSchemaId, opId);
        }
      },
    });
  }, [selectedSchemaId, redo, isMutating]);

  return { handleUndo, handleRedo, canUndo, canRedo };
};
