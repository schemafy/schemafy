import type { ServerResponse, SyncContext } from '../../types';
import { syncTempToRealIds } from './idSynchronizer';
import { syncPropagatedEntities } from './propagation/propagationManager';

export function handleServerResponse(
  response: ServerResponse,
  context: SyncContext,
) {
  if (!response.success || !response.result) return;

  const { result } = response;
  syncTempToRealIds(result, context);

  if (result.propagated) {
    syncPropagatedEntities(result.propagated, context);
  }
}
