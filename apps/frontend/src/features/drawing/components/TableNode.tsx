import { Handle, Position } from '@xyflow/react';
import { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { ulid } from 'ulid';
import { HANDLE_STYLE, type TableProps, type IndexType, type IndexSortDir } from '../types';
import { ColumnRow } from './Column';
import { TableHeader } from './TableHeader';
import { IndexSection } from './IndexSection';
import { useDragAndDrop, useColumnUpdate } from '../hooks';
import { ErdStore } from '@/store/erd.store';

const TableNodeComponent = ({ data, id }: TableProps) => {
  const erdStore = ErdStore.getInstance();
  const [isEditingTableName, setIsEditingTableName] = useState(false);
  const [isColumnEditMode, setIsColumnEditMode] = useState(false);
  const [editingTableName, setEditingTableName] = useState(data.tableName);

  const columns = data.columns || [];
  const indexes = data.indexes || [];

  const { updateColumn, saveAllPendingChanges } = useColumnUpdate(erdStore, data.schemaId, id);

  const dragAndDrop = useDragAndDrop({
    items: columns,
    onReorder: (newColumns) => {
      newColumns.forEach((column, index) => {
        erdStore.changeColumnPosition(data.schemaId, id, column.id, index);
      });
    },
  });

  const saveTableName = () => {
    erdStore.changeTableName(data.schemaId, id, editingTableName);
    setIsEditingTableName(false);
  };

  const addColumn = () => {
    erdStore.createColumn(data.schemaId, id, {
      id: ulid(),
      name: `newColumn${columns.length + 1}`,
      ordinalPosition: columns.length,
      dataType: 'VARCHAR',
      lengthScale: '255',
      isAutoIncrement: false,
      charset: 'utf8mb4',
      collation: 'utf8mb4_general_ci',
    });
    setIsColumnEditMode(true);
  };

  const removeColumn = (columnId: string) => {
    erdStore.deleteColumn(data.schemaId, id, columnId);
  };

  const deleteTable = () => {
    erdStore.deleteTable(data.schemaId, id);
  };

  const handleSaveAllPendingChanges = () => {
    saveAllPendingChanges();
    setIsColumnEditMode(false);
  };

  const createIndex = () => {
    erdStore.createIndex(data.schemaId, id, {
      id: ulid(),
      name: `idx_${data.tableName}_${indexes.length + 1}`,
      type: 'BTREE' as IndexType,
      comment: null,
      columns: [],
    });
  };

  const deleteIndex = (indexId: string) => {
    erdStore.deleteIndex(data.schemaId, id, indexId);
  };

  const changeIndexName = (indexId: string, newName: string) => {
    erdStore.changeIndexName(data.schemaId, id, indexId, newName);
  };

  const changeIndexType = (indexId: string, newType: IndexType) => {
    const index = indexes.find((idx) => idx.id === indexId);
    if (!index) return;

    erdStore.deleteIndex(data.schemaId, id, indexId);
    erdStore.createIndex(data.schemaId, id, {
      ...index,
      type: newType,
    });
  };

  const addColumnToIndex = (indexId: string, columnId: string) => {
    const index = indexes.find((idx) => idx.id === indexId);
    if (index) {
      erdStore.addColumnToIndex(data.schemaId, id, indexId, {
        id: ulid(),
        columnId,
        seqNo: index.columns.length + 1,
        sortDir: 'ASC' as IndexSortDir,
      });
    }
  };

  const removeColumnFromIndex = (indexId: string, indexColumnId: string) => {
    erdStore.removeColumnFromIndex(data.schemaId, id, indexId, indexColumnId);
  };

  const changeSortDir = (indexId: string, indexColumnId: string, sortDir: IndexSortDir) => {
    const index = indexes.find((idx) => idx.id === indexId);
    if (!index) return;

    const indexColumn = index.columns.find((col) => col.id === indexColumnId);
    if (!indexColumn) return;

    erdStore.deleteIndex(data.schemaId, id, indexId);
    erdStore.createIndex(data.schemaId, id, {
      ...index,
      columns: index.columns.map((col) => (col.id === indexColumnId ? { ...col, sortDir } : col)),
    });
  };

  return (
    <div className="group bg-schemafy-bg border-2 border-schemafy-button-bg rounded-lg shadow-md min-w-48 overflow-hidden">
      <ConnectionHandles nodeId={id} />
      <TableHeader
        tableName={data.tableName}
        isEditing={isEditingTableName}
        editingName={editingTableName}
        isColumnEditMode={isColumnEditMode}
        onStartEdit={() => setIsEditingTableName(true)}
        onSaveEdit={saveTableName}
        onCancelEdit={() => setIsEditingTableName(false)}
        onEditingNameChange={setEditingTableName}
        onToggleColumnEditMode={() => setIsColumnEditMode(!isColumnEditMode)}
        onSaveAllPendingChanges={handleSaveAllPendingChanges}
        onAddColumn={addColumn}
        onDeleteTable={deleteTable}
      />
      <div className="max-h-96 overflow-y-auto">
        {columns.map((column) => (
          <ColumnRow
            key={column.id}
            column={column}
            isEditMode={isColumnEditMode}
            isLastColumn={columns.length === 1}
            draggedItem={dragAndDrop.draggedItem}
            dragOverItem={dragAndDrop.dragOverItem}
            onDragStart={dragAndDrop.handleDragStart}
            onDragOver={dragAndDrop.handleDragOver}
            onDragLeave={dragAndDrop.handleDragLeave}
            onDrop={dragAndDrop.handleDrop}
            onDragEnd={dragAndDrop.handleDragEnd}
            onUpdateColumn={updateColumn}
            onRemoveColumn={removeColumn}
          />
        ))}
      </div>
      {columns.length === 0 && (
        <div className="p-4 text-center text-schemafy-dark-gray text-sm">Click + to add a column.</div>
      )}
      <IndexSection
        schemaId={data.schemaId}
        tableId={id}
        indexes={indexes}
        tableColumns={columns.map((col) => ({ id: col.id, name: col.name }))}
        isEditMode={isColumnEditMode}
        onCreateIndex={createIndex}
        onDeleteIndex={deleteIndex}
        onChangeIndexName={changeIndexName}
        onChangeIndexType={changeIndexType}
        onAddColumnToIndex={addColumnToIndex}
        onRemoveColumnFromIndex={removeColumnFromIndex}
        onChangeSortDir={changeSortDir}
      />
    </div>
  );
};

const ConnectionHandles = ({ nodeId }: { nodeId: string }) => {
  const handles = [
    {
      position: Position.Top,
      id: `${nodeId}-top-handle`,
    },
    {
      position: Position.Bottom,
      id: `${nodeId}-bottom-handle`,
    },
    {
      position: Position.Left,
      id: `${nodeId}-left-handle`,
    },
    {
      position: Position.Right,
      id: `${nodeId}-right-handle`,
    },
  ];

  return (
    <>
      {handles.map(({ position, id }) => (
        <Handle
          key={id}
          type={'source'}
          position={position}
          id={id}
          style={HANDLE_STYLE}
          className="group-hover:!opacity-100"
        />
      ))}
    </>
  );
};

export const TableNode = observer(TableNodeComponent);
