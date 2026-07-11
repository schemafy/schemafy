import { useCallback, useRef } from 'react';
import { useSelectedSchema } from '../contexts';
import { useUndo, useRedo } from './useOperationMutations';
import { operationHistoryStore } from '@/store/operation-history.store';

export const useUndoRedo = () => {
  const { selectedSchemaId } = useSelectedSchema();
  const { mutate: undo } = useUndo(selectedSchemaId);
  const { mutate: redo } = useRedo(selectedSchemaId);
  const redoStackRef = useRef<string[]>([]);

  const canUndo =
    operationHistoryStore.getUndoableOpIds(selectedSchemaId).length > 0;
  const canRedo = redoStackRef.current.length > 0;

  const handleUndo = useCallback(() => {
    const latest = operationHistoryStore.getLatestUndoableOperation(
      selectedSchemaId,
    );
    if (!latest?.opId) return;

    const opId = latest.opId;
    undo(opId, {
      onSuccess: () => {
        operationHistoryStore.removeUndoableOpId(selectedSchemaId, opId);
        redoStackRef.current.push(opId);
      },
      onError: () => {
        operationHistoryStore.clearSchemaHistory(selectedSchemaId);
        redoStackRef.current = [];
      },
    });
  }, [selectedSchemaId, undo]);

  const handleRedo = useCallback(() => {
    const opId = redoStackRef.current.pop();
    if (!opId) return;

    redo(opId, {
      onSuccess: () => {
        operationHistoryStore.addUndoableOpId(selectedSchemaId, opId);
      },
      onError: () => {
        operationHistoryStore.clearSchemaHistory(selectedSchemaId);
        redoStackRef.current = [];
      },
    });
  }, [selectedSchemaId, redo]);

  return { handleUndo, handleRedo, canUndo, canRedo };
};
