import { isAxiosError } from 'axios';

const KNOWN_UNDO_REDO_ERRORS = new Set([
  'OPERATION_NOT_FOUND',
  'OPERATION_SUPERSEDED',
  'OPERATION_ALREADY_UNDONE',
  'OPERATION_REDO_NOT_ELIGIBLE',
  'OPERATION_UNSUPPORTED',
]);

export const isKnownUndoRedoError = (error: unknown): boolean => {
  if (!isAxiosError<{ reason?: string }>(error)) return false;
  const reason = error.response?.data?.reason;
  return reason ? KNOWN_UNDO_REDO_ERRORS.has(reason) : false;
};
