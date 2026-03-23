import { useCallback, useEffect, useRef, useState } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
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

const isSameNodeList = (prev: Node<TableData>[], next: Node<TableData>[]) =>
  prev.length === next.length &&
  prev.every((node, index) => node === next[index]);

const hasSameTableNodeContent = (
  previousNode: Node<TableData>,
  nextNode: Node<TableData>,
) =>
  previousNode.position.x === nextNode.position.x &&
  previousNode.position.y === nextNode.position.y &&
  previousNode.data.schemaId === nextNode.data.schemaId &&
  JSON.stringify(previousNode.data) === JSON.stringify(nextNode.data);

export const useTables = () => {
  const { selectedSchemaId } = useSelectedSchema();
  const { data: snapshotsData } = useSchemaSnapshots(selectedSchemaId);

  const snapshotsRef = useLatest(snapshotsData);
  const previousSnapshotsRef = useRef<Record<
    string,
    TableSnapshotResponse
  > | null>(null);
  const { mutate: createTableWithExtra } =
    useCreateTableWithExtra(selectedSchemaId);
  const { mutate: changeTableExtra } = useChangeTableExtra(selectedSchemaId);
  const { mutate: deleteTable } = useDeleteTable(selectedSchemaId);

  const [tables, setTables] = useState<Node<TableData>[]>(() =>
    buildTableNodes(snapshotsData, selectedSchemaId),
  );

  useEffect(() => {
    const previousSnapshots = previousSnapshotsRef.current;
    previousSnapshotsRef.current = snapshotsData;

    setTables((previousTables) => {
      const previousNodesById = new Map(
        previousTables.map((table) => [table.id, table]),
      );

      const nextTables = Object.values(snapshotsData).map((snapshot) => {
        const previousNode = previousNodesById.get(snapshot.table.id);
        const previousSnapshot = previousSnapshots?.[snapshot.table.id];

        const nextNode = transformSnapshotToNode(snapshot, selectedSchemaId);

        if (
          previousNode &&
          previousSnapshot === snapshot &&
          previousNode.data.schemaId === selectedSchemaId
        ) {
          return previousNode;
        }

        if (previousNode && hasSameTableNodeContent(previousNode, nextNode)) {
          return previousNode;
        }

        return nextNode;
      });

      return isSameNodeList(previousTables, nextTables)
        ? previousTables
        : nextTables;
    });
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
        extra: JSON.stringify({ position }),
      });
    },
    [createTableWithExtra, selectedSchemaId],
  );

  const onNodeDragStop = useCallback(
    (_event: React.MouseEvent, node: Node<TableData>) => {
      const snapshot = snapshotsRef.current[node.id];
      if (!snapshot) return;

      const currentExtra = parseTableExtra(snapshot.table.extra);

      changeTableExtra({
        tableId: node.id,
        data: {
          extra: JSON.stringify({
            ...currentExtra,
            position: node.position,
          }),
        },
      });
    },
    [changeTableExtra],
  );

  const onTablesChange = useCallback((changes: NodeChange[]) => {
    setTables((nds) => applyNodeChanges(changes, nds) as Node<TableData>[]);
  }, []);

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
    onTablesChange,
    onNodeDragStop,
    onNodesDelete,
  };
};
