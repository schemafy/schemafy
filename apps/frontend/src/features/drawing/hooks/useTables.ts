import { useState, useEffect } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { toast } from 'sonner';
import type { TableData, Point } from '../types';
import {
  transformSnapshotToNode,
  parseTableExtra,
} from '../utils/tableHelpers';
import { generateUniqueName } from '../utils/nameGenerator';
import { useSelectedSchema } from '../contexts';
import { useSchemaSnapshots } from './useSchemaSnapshots';
import {
  useCreateTable,
  useChangeTableExtra,
  useDeleteTable,
} from './useTableMutations';

export const useTables = () => {
  const { selectedSchemaId } = useSelectedSchema();
  const { data: snapshotsData } = useSchemaSnapshots(selectedSchemaId);

  const createTableMutation = useCreateTable(selectedSchemaId);
  const changeTableExtraMutation = useChangeTableExtra(selectedSchemaId);
  const deleteTableMutation = useDeleteTable(selectedSchemaId);

  const [tables, setTables] = useState<Node<TableData>[]>([]);

  useEffect(() => {
    if (!snapshotsData) {
      setTables([]);
      return;
    }

    const newTables = Object.values(snapshotsData).map((snapshot) =>
      transformSnapshotToNode(snapshot, selectedSchemaId),
    );
    setTables(newTables);
  }, [selectedSchemaId, snapshotsData]);

  const addTable = (position: Point) => {
    if (!snapshotsData) {
      toast.error('No schema selected');
      return;
    }

    const existingTableNames = Object.values(snapshotsData).map(
      (snapshot) => snapshot.table.name,
    );

    const newTableName = generateUniqueName(existingTableNames, 'Table');

    createTableMutation.mutate(
      {
        schemaId: selectedSchemaId,
        name: newTableName,
        charset: '',
        collation: '',
      },
      {
        onSuccess: (result) => {
          if (result.data) {
            changeTableExtraMutation.mutate({
              tableId: result.data.id,
              data: {
                extra: JSON.stringify({ position }),
              },
            });
          }
        },
      },
    );
  };

  const onNodeDragStop = (_event: React.MouseEvent, node: Node<TableData>) => {
    if (!snapshotsData) return;

    const snapshot = snapshotsData[node.id];
    if (!snapshot) return;

    const currentExtra = parseTableExtra(snapshot.table.extra);

    changeTableExtraMutation.mutate({
      tableId: node.id,
      data: {
        extra: JSON.stringify({
          ...currentExtra,
          position: node.position,
        }),
      },
    });
  };

  const onTablesChange = (changes: NodeChange[]) => {
    setTables((nds) => applyNodeChanges(changes, nds) as Node<TableData>[]);
  };

  const onNodesDelete = (deletedNodes: Node<TableData>[]) => {
    deletedNodes.forEach((node) => {
      deleteTableMutation.mutate(node.id);
    });
  };

  return {
    tables,
    addTable,
    onTablesChange,
    onNodeDragStop,
    onNodesDelete,
  };
};
