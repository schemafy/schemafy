import { useCallback, useEffect, useState } from 'react';
import { type Node } from '@xyflow/react';
import type { TableData, Point } from '../types';
import {
  transformSnapshotToNode,
  parseTableExtra,
} from '../utils/tableHelpers';
import { generateUniqueName } from '../utils/nameGenerator';
import { useSelectedSchema } from '../contexts';
import { useSchemaSnapshots } from './useSchemaSnapshots';
import {
  useCreateTableWithExtra,
  useChangeTableExtra,
  useDeleteTable,
} from './useTableMutations';
import type { TableSnapshotResponse } from '../api';
import { useLatest } from './useLatest';

const buildTableNodes = (
  snapshotsData: Record<string, TableSnapshotResponse>,
  schemaId: string,
) =>
  Object.values(snapshotsData).map((snapshot) =>
    transformSnapshotToNode(snapshot, schemaId),
  );

export const useTables = () => {
  const { selectedSchemaId } = useSelectedSchema();
  const { data: snapshotsData } = useSchemaSnapshots(selectedSchemaId);

  const snapshotsRef = useLatest(snapshotsData);
  const { mutate: createTableWithExtra } =
    useCreateTableWithExtra(selectedSchemaId);
  const { mutate: changeTableExtra } = useChangeTableExtra(selectedSchemaId);
  const { mutate: deleteTable } = useDeleteTable(selectedSchemaId);

  const [tables, setTables] = useState<Node<TableData>[]>(() =>
    buildTableNodes(snapshotsData, selectedSchemaId),
  );

  useEffect(() => {
    setTables(buildTableNodes(snapshotsData, selectedSchemaId));
  }, [selectedSchemaId, snapshotsData]);

  const addTable = useCallback(
    (position: Point) => {
      const existingTableNames = Object.values(snapshotsRef.current).map(
        (snapshot) => snapshot.table.name,
      );

      const newTableName = generateUniqueName(existingTableNames, 'Table');
      createTableWithExtra({
        request: {
          schemaId: selectedSchemaId,
          name: newTableName,
          charset: '',
          collation: '',
        },
        extra: { position },
      });
    },
    [createTableWithExtra, selectedSchemaId, snapshotsRef],
  );

  const onNodeDragStop = useCallback(
    (_event: React.MouseEvent, node: Node<TableData>) => {
      const snapshot = snapshotsRef.current[node.id];
      if (!snapshot) return;

      const currentExtra = parseTableExtra(snapshot.table.extra);

      changeTableExtra({
        tableId: node.id,
        data: {
          extra: {
            ...currentExtra,
            position: node.position,
          },
        },
      });
    },
    [changeTableExtra, snapshotsRef],
  );

  const onNodesDelete = useCallback(
    (deletedNodes: Node<TableData>[]) => {
      deletedNodes.forEach((node) => {
        deleteTable(node.id);
      });
    },
    [deleteTable],
  );

  return {
    tables,
    addTable,
    onNodeDragStop,
    onNodesDelete,
  };
};
