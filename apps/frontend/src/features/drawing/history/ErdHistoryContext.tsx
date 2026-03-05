import { createContext, type ReactNode, useCallback, useState } from 'react';
import { toast } from 'sonner';
import type { ErdCommand } from './ErdCommand';

interface ErdHistoryContextValue {
  push: (command: ErdCommand) => void;
  undo: () => Promise<void>;
  redo: () => Promise<void>;
  clear: () => void;
  canUndo: boolean;
  canRedo: boolean;
  isProcessing: boolean;
}

export const ErdHistoryContext = createContext<ErdHistoryContextValue | null>(
  null,
);

const MAX_HISTORY_SIZE = 100;

export const ErdHistoryProvider = ({children}: { children: ReactNode }) => {
  const [undoStack, setUndoStack] = useState<ErdCommand[]>([]);
  const [redoStack, setRedoStack] = useState<ErdCommand[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);

  const push = useCallback((command: ErdCommand) => {
    setUndoStack((prev) => {
      const last = prev[prev.length - 1];
      if (last?.merge) {
        const merged = last.merge(command);
        if (merged) {
          return [...prev.slice(0, -1), merged];
        }
      }
      return [...prev.slice(-(MAX_HISTORY_SIZE - 1)), command];
    });
    setRedoStack([]);
  }, []);

  const undo = useCallback(async () => {
    if (isProcessing || undoStack.length === 0) return;
    const command = undoStack[undoStack.length - 1];
    setIsProcessing(true);
    try {
      await command.undo();
      setUndoStack((prev) => prev.slice(0, -1));
      setRedoStack((prev) => [...prev, command]);
    } catch {
      toast.error('실행 취소 중 오류가 발생했습니다.');
    } finally {
      setIsProcessing(false);
    }
  }, [undoStack, isProcessing]);

  const redo = useCallback(async () => {
    if (isProcessing || redoStack.length === 0) return;
    const command = redoStack[redoStack.length - 1];
    setIsProcessing(true);
    try {
      await command.redo();
      setRedoStack((prev) => prev.slice(0, -1));
      setUndoStack((prev) => [...prev, command]);
    } catch {
      toast.error('다시 실행 중 오류가 발생했습니다.');
    } finally {
      setIsProcessing(false);
    }
  }, [redoStack, isProcessing]);

  const clear = useCallback(() => {
    setUndoStack([]);
    setRedoStack([]);
  }, []);

  return (
    <ErdHistoryContext.Provider
      value={{
        push,
        undo,
        redo,
        clear,
        canUndo: undoStack.length > 0,
        canRedo: redoStack.length > 0,
        isProcessing,
      }}
    >
      {children}
    </ErdHistoryContext.Provider>
  );
};
