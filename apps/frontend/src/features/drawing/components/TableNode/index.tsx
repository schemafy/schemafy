import { useState } from 'react';
import { observer } from 'mobx-react-lite';
import type { TableProps } from '../../types';
import { ColumnRow } from '../Column';
import { TableHeader } from '../TableHeader';
import { IndexSection } from '../IndexSection';
import { useDragAndDrop, useColumnUpdate } from '../../hooks';
import { ErdStore } from '@/store/erd.store';
import { ConnectionHandles } from './ConnectionHandles';
import { useTableActions } from './useTableActions';
import { useColumnActions } from './useColumnActions';
import { useIndexActions } from './useIndexActions';

const TableNodeComponent = ({ data, id }: TableProps) => {
  const erdStore = ErdStore.getInstance();
  const [isColumnEditMode, setIsColumnEditMode] = useState(false);

  const columns = data.columns || [];
  const indexes = data.indexes || [];

  const { updateColumn, saveAllPendingChanges } = useColumnUpdate(erdStore, data.schemaId, id);

  const tableActions = useTableActions({
    erdStore,
    schemaId: data.schemaId,
    tableId: id,
    tableName: data.tableName,
  });

  const columnActions = useColumnActions({
    erdStore,
    schemaId: data.schemaId,
    tableId: id,
    columns,
  });

  const indexActions = useIndexActions({
    erdStore,
    schemaId: data.schemaId,
    tableId: id,
    tableName: data.tableName,
    indexes,
  });

  const dragAndDrop = useDragAndDrop({
    items: columns,
    onReorder: (newColumns) => {
      newColumns.forEach((column, index) => {
        erdStore.changeColumnPosition(data.schemaId, id, column.id, index);
      });
    },
  });

  const handleAddColumn = () => {
    columnActions.addColumn();
    setIsColumnEditMode(true);
  };

  const handleSaveAllPendingChanges = () => {
    saveAllPendingChanges();
    setIsColumnEditMode(false);
  };

  return (
    <div className="group bg-schemafy-bg border-2 border-schemafy-button-bg rounded-lg shadow-md min-w-48 overflow-hidden">
      <ConnectionHandles nodeId={id} />
      <TableHeader
        tableName={data.tableName}
        isEditing={tableActions.isEditingTableName}
        editingName={tableActions.editingTableName}
        isColumnEditMode={isColumnEditMode}
        onStartEdit={() => tableActions.setIsEditingTableName(true)}
        onSaveEdit={tableActions.saveTableName}
        onCancelEdit={() => tableActions.setIsEditingTableName(false)}
        onEditingNameChange={tableActions.setEditingTableName}
        onToggleColumnEditMode={() => setIsColumnEditMode(!isColumnEditMode)}
        onSaveAllPendingChanges={handleSaveAllPendingChanges}
        onAddColumn={handleAddColumn}
        onDeleteTable={tableActions.deleteTable}
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
            onRemoveColumn={columnActions.removeColumn}
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
        onCreateIndex={indexActions.createIndex}
        onDeleteIndex={indexActions.deleteIndex}
        onChangeIndexName={indexActions.changeIndexName}
        onChangeIndexType={indexActions.changeIndexType}
        onAddColumnToIndex={indexActions.addColumnToIndex}
        onRemoveColumnFromIndex={indexActions.removeColumnFromIndex}
        onChangeSortDir={indexActions.changeSortDir}
      />
    </div>
  );
};

export const TableNode = observer(TableNodeComponent);
