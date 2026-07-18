import { useCallback } from 'react';
import { toast } from 'sonner';
import { useSelectedSchema } from '../contexts';
import { useUndo, useRedo } from './useOperationMutations';
import { useErdCache } from './useErdCache';
import { operationHistoryStore } from '@/store/operation-history.store';
import { isKnownUndoRedoError } from '../constants/operationError';

export const useUndoRedo = () => {
  const { selectedSchemaId } = useSelectedSchema();
  const { mutate: undo } = useUndo();
  const { mutate: redo } = useRedo();
  const { syncAffectedTables } = useErdCache(selectedSchemaId);

  const isUndoRedoPending = operationHistoryStore.isPending(selectedSchemaId);
  const canUndo =
    operationHistoryStore.getUndoableOpIds(selectedSchemaId).length > 0 &&
    !isUndoRedoPending;
  const canRedo =
    operationHistoryStore.getRedoOpIds(selectedSchemaId).length > 0 &&
    !isUndoRedoPending;

  const handleUndo = useCallback(() => {
    if (operationHistoryStore.isPending(selectedSchemaId)) return;

    const latest =
      operationHistoryStore.getLatestUndoableOperation(selectedSchemaId);
    if (!latest?.opId) return;

    const opId = latest.opId;
    operationHistoryStore.setPending(selectedSchemaId);

    undo(opId, {
      onSuccess: async (result) => {
        operationHistoryStore.removeUndoableOpId(selectedSchemaId, opId);
        operationHistoryStore.pushRedoOpId(selectedSchemaId, opId);
        try {
          await syncAffectedTables(result);
        } finally {
          operationHistoryStore.clearPending(selectedSchemaId);
        }
      },
      onError: (error) => {
        toast.error('Undo or redo failed. Please try again.');
        if (isKnownUndoRedoError(error)) {
          operationHistoryStore.clearSchemaHistory(selectedSchemaId);
        }
        operationHistoryStore.clearPending(selectedSchemaId);
      },
    });
  }, [selectedSchemaId, undo, syncAffectedTables]);

  const handleRedo = useCallback(() => {
    if (operationHistoryStore.isPending(selectedSchemaId)) return;

    const opId = operationHistoryStore.popRedoOpId(selectedSchemaId);
    if (!opId) return;

    operationHistoryStore.setPending(selectedSchemaId);

    redo(opId, {
      onSuccess: async (result) => {
        operationHistoryStore.addUndoableOpId(selectedSchemaId, opId);
        try {
          await syncAffectedTables(result);
        } finally {
          operationHistoryStore.clearPending(selectedSchemaId);
        }
      },
      onError: (error) => {
        toast.error('Undo or redo failed. Please try again.');
        if (isKnownUndoRedoError(error)) {
          operationHistoryStore.clearSchemaHistory(selectedSchemaId);
        } else {
          operationHistoryStore.pushRedoOpId(selectedSchemaId, opId);
        }
        operationHistoryStore.clearPending(selectedSchemaId);
      },
    });
  }, [selectedSchemaId, redo, syncAffectedTables]);

  return { handleUndo, handleRedo, canUndo, canRedo };
};
