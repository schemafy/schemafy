import { createRoot } from 'react-dom/client';

import { EditModeIndex } from '@/features/drawing/components/IndexRow';
import { IndexSection } from '@/features/drawing/components/IndexSection';
import type { IndexCapabilities, IndexDataType } from '@/features/drawing/types';

const noop = () => {};

const baseIndex: IndexDataType = {
  id: 'index-1',
  tableId: 'table-1',
  name: 'idx_email',
  type: 'BTREE',
  columns: [
    {
      id: 'index-column-1',
      indexId: 'index-1',
      columnId: 'column-1',
      seqNo: 1,
      sortDir: 'ASC',
      isAffected: false,
    },
  ],
  isAffected: false,
};

const fulltextIndex: IndexDataType = {
  ...baseIndex,
  id: 'index-2',
  name: 'idx_body',
  type: 'FULLTEXT',
};

const loadingCapabilities: IndexCapabilities = {
  supportedTypes: [],
  sortDirectionTypes: [],
  status: 'loading',
};

// Simulates TanStack Query keeping stale data after a background refetch
// error: status is 'error' but supportedTypes still holds the last-successful
// (non-empty) payload.
const errorWithStaleDataCapabilities: IndexCapabilities = {
  supportedTypes: ['BTREE', 'FULLTEXT'],
  sortDirectionTypes: ['BTREE'],
  status: 'error',
};

const readyCapabilities: IndexCapabilities = {
  supportedTypes: ['BTREE', 'FULLTEXT'],
  sortDirectionTypes: ['BTREE'],
  status: 'ready',
};

const editModeScenarios: Array<{
  testId: string;
  index: IndexDataType;
  capabilities: IndexCapabilities;
}> = [
  { testId: 'loading', index: baseIndex, capabilities: loadingCapabilities },
  {
    testId: 'error-with-stale-data',
    index: baseIndex,
    capabilities: errorWithStaleDataCapabilities,
  },
  {
    testId: 'ready-unsupported-sortdir',
    index: fulltextIndex,
    capabilities: readyCapabilities,
  },
];

createRoot(document.getElementById('root')!).render(
  <>
    {editModeScenarios.map(({ testId, index, capabilities }) => (
      <div key={testId} data-testid={`edit-${testId}`}>
        <EditModeIndex
          index={index}
          tableColumns={[{ id: 'column-1', name: 'email' }]}
          indexCapabilities={capabilities}
          onDeleteIndex={noop}
          onUpdateIndexName={noop}
          onUpdateIndexType={noop}
          onAddColumnToIndex={noop}
          onRemoveColumnFromIndex={noop}
          onUpdateSortDir={noop}
        />
      </div>
    ))}
    <div data-testid="add-index-error-with-stale-data">
      <IndexSection
        schemaId="schema-1"
        tableId="table-1"
        indexes={[baseIndex]}
        tableColumns={[{ id: 'column-1', name: 'email' }]}
        isEditMode
        indexCapabilities={errorWithStaleDataCapabilities}
        onCreateIndex={noop}
        onDeleteIndex={noop}
        onUpdateIndexName={noop}
        onUpdateIndexType={noop}
        onAddColumnToIndex={noop}
        onRemoveColumnFromIndex={noop}
        onUpdateSortDir={noop}
      />
    </div>
  </>,
);
