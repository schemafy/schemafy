import { useCallback, useRef } from 'react';
import { useSelectedSchema } from '../contexts';
import { useUndo, useRedo } from './useOperationMutations';
import { operationHistoryStore } from '@/store/operation-history.store';

export const useUndoRedo = () => {
  const { selectedSchemaId } = useSelectedSchema();
  const { mutate: undo } = useUndo(selectedSchemaId);
  const { mutate: redo } = useRedo(selectedSchemaId);
  const isPendingRef = useRef(false);

  const canUndo =
    operationHistoryStore.getUndoableOpIds(selectedSchemaId).length > 0;
  const canRedo =
    operationHistoryStore.getRedoOpIds(selectedSchemaId).length > 0;

  const handleUndo = useCallback(() => {
    if (isPendingRef.current) return;

    const latest = operationHistoryStore.getLatestUndoableOperation(
      selectedSchemaId,
    );
    if (!latest?.opId) return;

    const opId = latest.opId;
    isPendingRef.current = true;

    undo(opId, {
      onSuccess: () => {
        operationHistoryStore.removeUndoableOpId(selectedSchemaId, opId);
        operationHistoryStore.pushRedoOpId(selectedSchemaId, opId);
        isPendingRef.current = false;
      },
      onError: () => {
        operationHistoryStore.clearSchemaHistory(selectedSchemaId);
        isPendingRef.current = false;
      },
    });
  }, [selectedSchemaId, undo]);

  const handleRedo = useCallback(() => {
    if (isPendingRef.current) return;

    const opId = operationHistoryStore.popRedoOpId(selectedSchemaId);
    if (!opId) return;

    isPendingRef.current = true;

    redo(opId, {
      onSuccess: () => {
        operationHistoryStore.addUndoableOpId(selectedSchemaId, opId);
        isPendingRef.current = false;
      },
      onError: () => {
        operationHistoryStore.clearSchemaHistory(selectedSchemaId);
        isPendingRef.current = false;
      },
    });
  }, [selectedSchemaId, redo]);

  return { handleUndo, handleRedo, canUndo, canRedo };
};
