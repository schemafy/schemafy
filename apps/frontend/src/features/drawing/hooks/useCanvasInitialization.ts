import { useEffect } from 'react';
import { ulid } from 'ulid';
import { ErdStore } from '@/store/erd.store';
import { collaborationStore } from '@/store/collaboration.store';

export const useCanvasInitialization = () => {
  const erdStore = ErdStore.getInstance();

  useEffect(() => {
    if (erdStore.erdState.state === 'idle') {
      const dbId = ulid();
      const schemaId = ulid();
      erdStore.load({
        id: dbId,
        isAffected: false,
        schemas: [
          {
            id: schemaId,
            projectId: ulid(),
            dbVendorId: 'MYSQL',
            name: 'schema1',
            charset: 'utf8mb4',
            collation: 'utf8mb4_general_ci',
            vendorOption: '',
            tables: [],
            isAffected: false,
          },
        ],
      });
    }
  }, [erdStore]);

  useEffect(() => {
    collaborationStore.connect('06DS8JSJ7Y112MC87X0AB2CE8M');

    return () => {
      collaborationStore.disconnect();
    };
  }, []);
};
