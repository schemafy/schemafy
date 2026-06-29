import { useCallback, useEffect, useRef, useState } from 'react';
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
import { collaborationStore } from '@/store/collaboration.store';
import { useThrottledCallback } from '@/hooks/useThrottledCallback';

const TABLE_POSITION_PREVIEW_THROTTLE_MS = 50;
const TABLE_POSITION_PREVIEW_CLEAR_DELAY_MS = 500;

const buildTableNodes = (
  snapshotsData: Record<string, TableSnapshotResponse>,
  schemaId: string,
) =>
  Object.values(snapshotsData).map((snapshot) =>
    transformSnapshotToNode(snapshot, schemaId),
  );

export const useTables = () => {
  const { selectedSchemaId } = useSelectedSchema();
  const { data: schemaSnapshots } = useSchemaSnapshots(selectedSchemaId);
  const snapshotsData = schemaSnapshots.snapshots;

  const snapshotsRef = useLatest(snapshotsData);
  const clearPreviewTimerRef = useRef<number | null>(null);
  const previousSnapshotsRef = useRef<Record<
    string,
    TableSnapshotResponse
  > | null>(null);
  const previousSchemaIdRef = useRef(selectedSchemaId);
  const { mutate: createTableWithExtra } =
    useCreateTableWithExtra(selectedSchemaId);
  const { mutate: changeTableExtra } = useChangeTableExtra(selectedSchemaId);
  const { mutate: deleteTable } = useDeleteTable(selectedSchemaId);

  const [tables, setTables] = useState<Node<TableData>[]>(() =>
    buildTableNodes(snapshotsData, selectedSchemaId),
  );

  useEffect(() => {
    return () => {
      if (clearPreviewTimerRef.current !== null) {
        window.clearTimeout(clearPreviewTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const previousSnapshots = previousSnapshotsRef.current;
    const schemaChanged = previousSchemaIdRef.current !== selectedSchemaId;

    previousSnapshotsRef.current = snapshotsData;
    previousSchemaIdRef.current = selectedSchemaId;

    setTables((previousTables) => {
      if (!previousSnapshots || schemaChanged) {
        return buildTableNodes(snapshotsData, selectedSchemaId);
      }

      const previousNodesById = new Map(
        previousTables.map((table) => [table.id, table]),
      );

      Object.values(snapshotsData).forEach((snapshot) => {
        const tableId = snapshot.table.id;
        const previousSnapshot = previousSnapshots[tableId];

        if (!previousSnapshot || previousSnapshot !== snapshot) {
          previousNodesById.set(
            tableId,
            transformSnapshotToNode(snapshot, selectedSchemaId),
          );
        }
      });

      return Object.values(snapshotsData).map(
        (snapshot) => previousNodesById.get(snapshot.table.id)!,
      );
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
        extra: { position },
      });
    },
    [createTableWithExtra, selectedSchemaId, snapshotsRef],
  );

  const sendTablePositionPreview = useThrottledCallback(
    (tableId: string, position: Point) => {
      collaborationStore.sendTablePositionPreview(
        selectedSchemaId,
        tableId,
        position,
      );
    },
    TABLE_POSITION_PREVIEW_THROTTLE_MS,
  );

  const onNodeDrag = useCallback(
    (_event: React.MouseEvent, node: Node<TableData>) => {
      sendTablePositionPreview(node.id, node.position);
    },
    [sendTablePositionPreview],
  );

  const onNodeDragStop = useCallback(
    (_event: React.MouseEvent, node: Node<TableData>) => {
      const snapshot = snapshotsRef.current[node.id];
      if (!snapshot) return;

      collaborationStore.sendTablePositionPreview(
        selectedSchemaId,
        node.id,
        node.position,
      );

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

      if (clearPreviewTimerRef.current !== null) {
        window.clearTimeout(clearPreviewTimerRef.current);
      }
      clearPreviewTimerRef.current = window.setTimeout(() => {
        collaborationStore.sendTablePositionPreview(
          selectedSchemaId,
          node.id,
          null,
        );
        clearPreviewTimerRef.current = null;
      }, TABLE_POSITION_PREVIEW_CLEAR_DELAY_MS);
    },
    [changeTableExtra, selectedSchemaId, snapshotsRef],
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
    onNodeDrag,
    onNodeDragStop,
    onNodesDelete,
  };
};
