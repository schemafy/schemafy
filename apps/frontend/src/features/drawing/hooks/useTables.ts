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
import { previewStore } from '@/store/preview.store';
import type { PreviewEntry, TablePositionPreviewEntry } from '@/store';
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

const applyTablePositionPreviews = (
  nodes: Node<TableData>[],
  previewPositions: Map<string, Point>,
) => {
  if (previewPositions.size === 0) return nodes;

  return nodes.map((node) => {
    const position = previewPositions.get(node.id);
    if (!position) return node;

    if (node.position.x === position.x && node.position.y === position.y) {
      return node;
    }

    return {
      ...node,
      position,
    };
  });
};

const isTablePositionPreview = (
  entry: PreviewEntry,
): entry is TablePositionPreviewEntry => entry.kind === 'TABLE_POSITION';

const getTablePositionPreviewMap = (schemaId: string) => {
  const previewPositions = new Map<string, Point>();

  for (const entry of previewStore.previews.values()) {
    if (isTablePositionPreview(entry) && entry.schemaId === schemaId) {
      previewPositions.set(entry.tableId, entry.position);
    }
  }

  return previewPositions;
};

export const useTables = (canEditProject: boolean) => {
  const { selectedSchemaId } = useSelectedSchema();
  const { data: schemaSnapshots } = useSchemaSnapshots(selectedSchemaId);
  const snapshotsData = schemaSnapshots.snapshots;
  const tablePositionPreviewEntries = [
    ...previewStore.previews.values(),
  ].filter(
    (entry): entry is TablePositionPreviewEntry =>
      isTablePositionPreview(entry) && entry.schemaId === selectedSchemaId,
  );
  const tablePositionPreviewKey = tablePositionPreviewEntries
    .map((entry) => `${entry.tableId}:${entry.position.x}:${entry.position.y}`)
    .sort()
    .join('|');

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
    applyTablePositionPreviews(
      buildTableNodes(snapshotsData, selectedSchemaId),
      getTablePositionPreviewMap(selectedSchemaId),
    ),
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
    const tablePositionPreviewMap =
      getTablePositionPreviewMap(selectedSchemaId);

    setTables((previousTables) => {
      if (!previousSnapshots || schemaChanged) {
        return applyTablePositionPreviews(
          buildTableNodes(snapshotsData, selectedSchemaId),
          tablePositionPreviewMap,
        );
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

      return applyTablePositionPreviews(
        Object.values(snapshotsData).map(
          (snapshot) => previousNodesById.get(snapshot.table.id)!,
        ),
        tablePositionPreviewMap,
      );
    });
  }, [
    selectedSchemaId,
    snapshotsData,
    tablePositionPreviewKey,
  ]);

  const addTable = useCallback(
    (position: Point) => {
      if (!canEditProject) return;

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
    [canEditProject, createTableWithExtra, selectedSchemaId, snapshotsRef],
  );

  const sendTablePositionPreview = useThrottledCallback(
    (tableId: string, position: Point) => {
      if (!canEditProject) return;

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
      if (!canEditProject) return;

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
    [canEditProject, changeTableExtra, selectedSchemaId, snapshotsRef],
  );

  const onNodesDelete = useCallback(
    (deletedNodes: Node<TableData>[]) => {
      if (!canEditProject) return;

      deletedNodes.forEach((node) => {
        deleteTable(node.id);
      });
    },
    [canEditProject, deleteTable],
  );

  return {
    tables,
    addTable,
    onNodeDrag,
    onNodeDragStop,
    onNodesDelete,
  };
};
