import { isAxiosError } from 'axios';

const KNOWN_UNDO_REDO_ERRORS = new Set([
  'OPERATION_NOT_FOUND',
  'OPERATION_SUPERSEDED',
  'OPERATION_ALREADY_UNDONE',
  'OPERATION_REDO_NOT_ELIGIBLE',
  'OPERATION_UNSUPPORTED',
]);

export const isKnownUndoRedoError = (error: unknown): boolean => {
  if (!isAxiosError(error)) return false;

  const data = error.response?.data as
    | Record<string, string | undefined>
    | undefined;
  if (!data) return false;

  const errorCode = data.code ?? data.reason;
  return errorCode ? KNOWN_UNDO_REDO_ERRORS.has(errorCode) : false;
};
