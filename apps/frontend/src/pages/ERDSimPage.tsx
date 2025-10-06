import { useMemo, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { Button } from '@/components';
import { erdStore } from '@/store';

type IdPrefix =
  | 'db'
  | 'schema'
  | 'table'
  | 'col'
  | 'idx'
  | 'idxcol'
  | 'constraint'
  | 'cc'
  | 'rel'
  | 'relcol';

const genId = (prefix: IdPrefix) =>
  `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;

export const ERDSimPage = observer(() => {
  const [error, setError] = useState<string | null>(null);

  const schemaId = useMemo(
    () => erdStore.database?.schemas[0]?.id ?? null,
    [erdStore.database],
  );

  const loadInitial = () => {
    setError(null);
    const dbId = genId('db');
    const schId = genId('schema');
    erdStore.load({
      id: dbId,
      schemas: [
        {
          id: schId,
          projectId: genId('schema'),
          dbVendorId: 'mysql',
          name: 'public',
          charset: 'utf8mb4',
          collation: 'utf8mb4_general_ci',
          vendorOption: '',
          createdAt: new Date(),
          updatedAt: new Date(),
          deletedAt: null,
          tables: [],
        },
      ],
    });
  };

  const createUsersTable = () => {
    if (!schemaId) return;
    try {
      erdStore.createTable(schemaId, {
        id: genId('table'),
        name: 'users',
        comment: null,
        tableOptions: '',
        deletedAt: null,
        columns: [],
        indexes: [],
        constraints: [],
        relationships: [],
      });
    } catch (e: Error | unknown) {
      setError(e instanceof Error ? e.message : 'Unknown error');
    }
  };

  const addUsersIdColumn = () => {
    if (!schemaId || !erdStore.database) return;
    const users = erdStore.database.schemas[0].tables.find(
      (t) => t.name === 'users',
    );
    if (!users) return;
    try {
      erdStore.createColumn(schemaId, users.id, {
        id: genId('col'),
        name: 'id2',
        ordinalPosition: users.columns.length + 1,
        dataType: 'INT',
        lengthScale: '',
        isAutoIncrement: true,
        charset: 'utf8mb4',
        collation: 'utf8mb4_general_ci',
        comment: null,
        deletedAt: null,
      });
    } catch (e: Error | unknown) {
      setError(e instanceof Error ? e.message : 'Unknown error');
    }
  };

  const setUsersPk = () => {
    if (!schemaId || !erdStore.database) return;
    const users = erdStore.database.schemas[0].tables.find(
      (t) => t.name === 'users',
    );
    const idCol = users?.columns.find((c) => c.name === 'id2');
    if (!users || !idCol) return;
    try {
      const pkId = genId('constraint');
      erdStore.createConstraint(schemaId, users.id, {
        id: pkId,
        name: 'pk_users',
        kind: 'PRIMARY_KEY',
        checkExpr: null,
        defaultExpr: null,
        columns: [
          {
            id: genId('cc'),
            constraintId: pkId,
            columnId: idCol.id,
            seqNo: 1,
          },
        ],
      });
    } catch (e: Error | unknown) {
      setError(e instanceof Error ? e.message : 'Unknown error');
    }
  };

  const createPostsTable = () => {
    if (!schemaId) return;
    try {
      erdStore.createTable(schemaId, {
        id: genId('table'),
        name: 'posts',
        comment: null,
        tableOptions: '',
        deletedAt: null,
        columns: [],
        indexes: [],
        constraints: [],
        relationships: [],
      });
    } catch (e: Error | unknown) {
      setError(e instanceof Error ? e.message : 'Unknown error');
    }
  };

  const addPostsIdColumn = () => {
    if (!schemaId || !erdStore.database) return;
    const posts = erdStore.database.schemas[0].tables.find(
      (t) => t.name === 'posts',
    );
    if (!posts) return;
    try {
      erdStore.createColumn(schemaId, posts.id, {
        id: genId('col'),
        name: 'id2',
        ordinalPosition: posts.columns.length + 1,
        dataType: 'INT',
        lengthScale: '',
        isAutoIncrement: true,
        charset: 'utf8mb4',
        collation: 'utf8mb4_general_ci',
        comment: null,
        deletedAt: null,
      });
    } catch (e: Error | unknown) {
      setError(e instanceof Error ? e.message : 'Unknown error');
    }
  };

  const relatePostsToUsers = () => {
    if (!schemaId || !erdStore.database) return;
    const schema = erdStore.database.schemas[0];
    const users = schema.tables.find((t) => t.name === 'users');
    const posts = schema.tables.find((t) => t.name === 'posts');
    const usersPk = users?.constraints.find((c) => c.kind === 'PRIMARY_KEY');
    const usersPkColId = usersPk?.columns[0]?.columnId;
    if (!users || !posts || !usersPkColId) return;
    try {
      const relId = genId('rel');
      erdStore.createRelationship(schemaId, {
        id: relId,
        srcTableId: posts.id,
        tgtTableId: users.id,
        name: 'fk_posts_users',
        kind: 'IDENTIFYING',
        cardinality: '1:N',
        onDelete: 'CASCADE',
        onUpdate: 'CASCADE',
        fkEnforced: false,
        columns: [
          {
            id: genId('relcol'),
            relationshipId: relId,
            fkColumnId: genId('col'),
            refColumnId: usersPkColId,
            seqNo: 1,
          },
        ],
      });
    } catch (e: Error | unknown) {
      setError(e instanceof Error ? e.message : 'Unknown error');
    }
  };

  const addIndexOnPostsFirstColumn = () => {
    if (!schemaId || !erdStore.database) return;
    const posts = erdStore.database.schemas[0].tables.find(
      (t) => t.name === 'posts',
    );
    const firstCol = posts?.columns[0];
    if (!posts || !firstCol) return;
    try {
      const idxId = genId('idx');
      erdStore.createIndex(schemaId, posts.id, {
        id: idxId,
        name: 'idx_posts_col1',
        type: 'BTREE',
        comment: null,
        columns: [
          {
            id: genId('idxcol'),
            indexId: idxId,
            columnId: firstCol.id,
            seqNo: 1,
            sortDir: 'ASC',
          },
        ],
      });
    } catch (e: Error | unknown) {
      setError(e instanceof Error ? e.message : 'Unknown error');
    }
  };

  const renameUsersToMembers = () => {
    if (!schemaId || !erdStore.database) return;
    const users = erdStore.database.schemas[0].tables.find(
      (t) => t.name === 'users',
    );
    if (!users) return;
    try {
      erdStore.changeTableName(schemaId, users.id, 'members');
    } catch (e: Error | unknown) {
      setError(e instanceof Error ? e.message : 'Unknown error');
    }
  };

  const runValidate = () => {
    try {
      erdStore.validate();
      setError(null);
    } catch (e: Error | unknown) {
      setError(e instanceof Error ? e.message : 'Unknown error');
    }
  };

  const reset = () => {
    erdStore.reset();
    setError(null);
  };

  const dbJson = useMemo(
    () => JSON.stringify(erdStore.database, null, 2),
    [erdStore.database],
  );

  return (
    <div className="flex flex-col gap-4 p-4">
      <h2 className="font-display-md">ERD Simulation</h2>
      <div className="flex gap-2 flex-wrap">
        <Button onClick={loadInitial}>1) Load initial</Button>
        <Button onClick={createUsersTable}>2) Create table users</Button>
        <Button onClick={addUsersIdColumn}>3) Add users.id</Button>
        <Button onClick={setUsersPk}>4) Set PK(users.id)</Button>
        <Button onClick={createPostsTable}>5) Create table posts</Button>
        <Button onClick={addPostsIdColumn}>6) Add posts.id</Button>
        <Button onClick={relatePostsToUsers}>7) Relate posts to users</Button>
        <Button onClick={addIndexOnPostsFirstColumn}>
          8) Add index on posts
        </Button>
        <Button onClick={renameUsersToMembers}>
          9) Rename users to members
        </Button>
        <Button onClick={runValidate}>Validate</Button>
        <Button onClick={reset} variant="secondary">
          Reset
        </Button>
      </div>

      {error && <div className="text-red-600 text-sm">Error: {error}</div>}

      <div className="border p-2 rounded">
        <h3 className="font-heading-sm mb-2">Database JSON</h3>
        <pre className="text-xs overflow-auto max-h-[420px]">{dbJson}</pre>
      </div>
    </div>
  );
});
