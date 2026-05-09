import { action, computed, makeObservable, observable } from 'mobx';
import type { ErdOperation } from '@/features/drawing/api/types';
import type { ReceiveErdMutated } from '@/features/collaboration/api';
import type {
  LocalOperationMetadata,
  LocalOperationStatus,
  PendingOperationMetadata,
} from '@/types/operation.types';

export class OperationHistoryStore {
  operationsByClientId: Map<string, LocalOperationMetadata> = new Map();
  clientIdsByOpId: Map<string, string> = new Map();
  undoableOpIdsBySchemaId: Map<string, string[]> = new Map();
  lastRemoteOperationBySchemaId: Map<string, ErdOperation> = new Map();

  constructor() {
    makeObservable(this, {
      operationsByClientId: observable.shallow,
      clientIdsByOpId: observable.shallow,
      undoableOpIdsBySchemaId: observable.shallow,
      lastRemoteOperationBySchemaId: observable.shallow,
      pendingOperations: computed,
      undoableOperations: computed,
      markPending: action,
      markUndoable: action,
      markFailed: action,
      handleErdMutated: action,
      clearSchemaHistory: action,
      clearAll: action,
    });
  }

  get pendingOperations() {
    return this.filterByStatus('pending');
  }

  get undoableOperations() {
    return this.filterByStatus('undoable');
  }

  markPending(metadata: PendingOperationMetadata) {
    const existing = this.operationsByClientId.get(metadata.clientOperationId);

    if (existing) {
      const operation: LocalOperationMetadata = {
        ...existing,
        schemaId: existing.schemaId ?? metadata.schemaId,
        baseSchemaRevision:
          existing.baseSchemaRevision ?? metadata.baseSchemaRevision,
      };

      this.operationsByClientId.set(metadata.clientOperationId, operation);
      return;
    }

    const next: LocalOperationMetadata = {
      ...metadata,
      opId: null,
      committedRevision: null,
      derivationKind: null,
      status: 'pending',
      affectedTableIds: [],
      failureMessage: null,
      createdAt: Date.now(),
      committedAt: null,
    };

    this.operationsByClientId.set(metadata.clientOperationId, next);
  }

  markUndoable(
    schemaId: string,
    operation: ErdOperation,
    affectedTableIds: string[],
  ) {
    const clientOperationId = operation.clientOperationId;

    if (!clientOperationId) return;

    const existing = this.operationsByClientId.get(clientOperationId);
    const next: LocalOperationMetadata = {
      clientOperationId,
      schemaId,
      baseSchemaRevision: existing?.baseSchemaRevision ?? null,
      opId: operation.opId,
      committedRevision: operation.committedRevision,
      derivationKind: operation.derivationKind,
      status: 'undoable',
      affectedTableIds,
      failureMessage: null,
      createdAt: existing?.createdAt ?? Date.now(),
      committedAt: existing?.committedAt ?? Date.now(),
    };

    this.operationsByClientId.set(clientOperationId, next);
    this.clientIdsByOpId.set(operation.opId, clientOperationId);

    const undoableOpIds = this.undoableOpIdsBySchemaId.get(schemaId) ?? [];

    if (!undoableOpIds.includes(operation.opId)) {
      this.undoableOpIdsBySchemaId.set(schemaId, [
        ...undoableOpIds,
        operation.opId,
      ]);
    }
  }

  markFailed(clientOperationId: string, error?: unknown) {
    const existing = this.operationsByClientId.get(clientOperationId);

    if (!existing || existing.status === 'undoable') return;

    this.operationsByClientId.set(clientOperationId, {
      ...existing,
      status: 'failed',
      failureMessage: this.errorMessage(error),
    });
  }

  handleErdMutated(message: ReceiveErdMutated) {
    const clientOperationId = message.operation.clientOperationId;

    if (clientOperationId && this.operationsByClientId.has(clientOperationId)) {
      return;
    }

    this.lastRemoteOperationBySchemaId.set(message.schemaId, message.operation);
    this.clearSchemaUndoableHistory(message.schemaId);
  }

  getUndoableOpIds(schemaId: string) {
    return this.undoableOpIdsBySchemaId.get(schemaId) ?? [];
  }

  getLatestUndoableOperation(schemaId: string) {
    const opIds = this.getUndoableOpIds(schemaId);
    const opId = opIds.at(-1);

    if (!opId) return null;

    const clientOperationId = this.clientIdsByOpId.get(opId);

    if (!clientOperationId) return null;

    return this.operationsByClientId.get(clientOperationId) ?? null;
  }

  clearSchemaHistory(schemaId: string) {
    [...this.operationsByClientId.entries()].forEach(
      ([clientOperationId, operation]) => {
        if (operation.schemaId === schemaId) {
          this.operationsByClientId.delete(clientOperationId);
          if (operation.opId) {
            this.clientIdsByOpId.delete(operation.opId);
          }
        }
      },
    );
    this.undoableOpIdsBySchemaId.delete(schemaId);
    this.lastRemoteOperationBySchemaId.delete(schemaId);
  }

  clearAll() {
    this.operationsByClientId.clear();
    this.clientIdsByOpId.clear();
    this.undoableOpIdsBySchemaId.clear();
    this.lastRemoteOperationBySchemaId.clear();
  }

  private filterByStatus(status: LocalOperationStatus) {
    return [...this.operationsByClientId.values()].filter(
      (operation) => operation.status === status,
    );
  }

  private errorMessage(error: unknown) {
    if (error instanceof Error) return error.message;
    if (typeof error === 'string') return error;
    return null;
  }

  private clearSchemaUndoableHistory(schemaId: string) {
    const undoableOpIds = this.undoableOpIdsBySchemaId.get(schemaId) ?? [];

    undoableOpIds.forEach((opId) => {
      const clientOperationId = this.clientIdsByOpId.get(opId);

      if (!clientOperationId) return;

      this.operationsByClientId.delete(clientOperationId);
      this.clientIdsByOpId.delete(opId);
    });

    this.undoableOpIdsBySchemaId.delete(schemaId);
  }
}

export const operationHistoryStore = new OperationHistoryStore();
