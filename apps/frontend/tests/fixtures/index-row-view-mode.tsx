import { createRoot } from 'react-dom/client';

import { ViewModeIndex } from '@/features/drawing/components/IndexRow';
import type { IndexCapabilitiesStatus } from '@/features/drawing/types';

const statuses: IndexCapabilitiesStatus[] = ['loading', 'error'];

const index = {
  id: 'index-1',
  tableId: 'table-1',
  name: 'idx_email',
  type: 'FULLTEXT' as const,
  columns: [
    {
      id: 'index-column-1',
      indexId: 'index-1',
      columnId: 'column-1',
      seqNo: 1,
      sortDir: 'DESC' as const,
      isAffected: false,
    },
  ],
  isAffected: false,
};

createRoot(document.getElementById('root')!).render(
  <>
    {statuses.map((status) => (
      <div key={status} data-testid={`index-${status}`}>
        <ViewModeIndex
          index={index}
          tableColumns={[{ id: 'column-1', name: 'email' }]}
          indexCapabilities={{
            supportedTypes: [],
            sortDirectionTypes: [],
            status,
          }}
        />
      </div>
    ))}
  </>,
);
