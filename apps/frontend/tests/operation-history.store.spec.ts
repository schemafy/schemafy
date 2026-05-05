import { expect, test } from '@playwright/test';
import { OperationHistoryStore } from '../src/store/operation-history.store';

const operation = (clientOperationId: string, opId = 'op-1') => ({
  opId,
  clientOperationId,
  committedRevision: 2,
  derivationKind: 'ORIGINAL',
});

test.describe('OperationHistoryStore', () => {
  test('pending operation을 등록한다', () => {
    const store = new OperationHistoryStore();

    store.markPending({
      clientOperationId: 'client-op-1',
      schemaId: 'schema-1',
      baseSchemaRevision: 1,
    });

    expect(store.pendingOperations).toHaveLength(1);
    expect(store.pendingOperations[0]).toMatchObject({
      clientOperationId: 'client-op-1',
      schemaId: 'schema-1',
      baseSchemaRevision: 1,
      status: 'pending',
    });
  });

  test('HTTP success 이후 committed로 전환한다', () => {
    const store = new OperationHistoryStore();

    store.markPending({
      clientOperationId: 'client-op-1',
      schemaId: 'schema-1',
      baseSchemaRevision: 1,
    });
    store.markCommitted('schema-1', operation('client-op-1'), ['table-1']);

    expect(store.pendingOperations).toHaveLength(0);
    expect(store.committedOperations).toHaveLength(1);
    expect(store.committedOperations[0]).toMatchObject({
      clientOperationId: 'client-op-1',
      opId: 'op-1',
      committedRevision: 2,
      affectedTableIds: ['table-1'],
      status: 'committed',
    });
  });

  test('matching ERD_MUTATED 수신은 local undoable 상태를 변경하지 않는다', () => {
    const store = new OperationHistoryStore();

    store.markPending({
      clientOperationId: 'client-op-1',
      schemaId: 'schema-1',
      baseSchemaRevision: 1,
    });
    store.markCommitted('schema-1', operation('client-op-1'), ['table-1']);
    store.handleErdMutated({
      type: 'ERD_MUTATED',
      schemaId: 'schema-1',
      sessionId: 'session-1',
      affectedTableIds: ['table-1'],
      operation: operation('client-op-1'),
      timestamp: Date.now(),
    });

    expect(store.committedOperations).toHaveLength(1);
    expect(store.undoableOperations).toHaveLength(0);
    expect(store.getUndoableOpIds('schema-1')).toEqual([]);
    expect(store.getLatestUndoableOperation('schema-1')).toBeNull();
    expect(store.committedOperations[0]).toMatchObject({
      clientOperationId: 'client-op-1',
      opId: 'op-1',
      status: 'committed',
    });
  });

  test('HTTP error 이후 failed로 전환한다', () => {
    const store = new OperationHistoryStore();

    store.markPending({
      clientOperationId: 'client-op-1',
      schemaId: 'schema-1',
      baseSchemaRevision: 1,
    });
    store.markFailed('client-op-1', new Error('request failed'));

    expect(store.pendingOperations).toHaveLength(0);
    expect(store.operationsByClientId.get('client-op-1')).toMatchObject({
      status: 'failed',
      failureMessage: 'request failed',
    });
  });

  test('unknown remote operation은 local pending을 오염시키지 않는다', () => {
    const store = new OperationHistoryStore();

    store.markPending({
      clientOperationId: 'client-op-1',
      schemaId: 'schema-1',
      baseSchemaRevision: 1,
    });
    store.handleErdMutated({
      type: 'ERD_MUTATED',
      schemaId: 'schema-1',
      sessionId: 'session-2',
      affectedTableIds: ['table-2'],
      operation: operation('remote-client-op', 'remote-op-1'),
      timestamp: Date.now(),
    });

    expect(store.pendingOperations).toHaveLength(1);
    expect(store.undoableOperations).toHaveLength(0);
    expect(store.lastRemoteOperationBySchemaId.get('schema-1')).toMatchObject({
      opId: 'remote-op-1',
      clientOperationId: 'remote-client-op',
    });
  });
});
