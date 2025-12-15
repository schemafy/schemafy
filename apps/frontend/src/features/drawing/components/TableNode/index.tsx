import { useState } from 'react';
import { observer } from 'mobx-react-lite';
import type { TableProps } from '../../types';
import { ColumnRow } from '../Column';
import { TableHeader } from '../TableHeader';
import { IndexSection } from '../IndexSection';
import { ConstraintSection } from '../ConstraintSection';
import {
  useDragAndDrop,
  useColumn,
  useTable,
  useColumns,
  useIndexes,
  useConstraints,
} from '../../hooks';
import { ErdStore } from '@/store/erd.store';
import { ConnectionHandles } from './ConnectionHandles';
import * as columnService from '../../services/column.service';

const TableNodeComponent = ({ data, id }: TableProps) => {
  const erdStore = ErdStore.getInstance();
  const [isColumnEditMode, setIsColumnEditMode] = useState(false);

  const columns = data.columns || [];
  const indexes = data.indexes || [];
  const constraints = data.constraints || [];

  const { updateColumn, saveAllPendingChanges } = useColumn(
    erdStore,
    data.schemaId,
    id,
  );

  const tableActions = useTable({
    schemaId: data.schemaId,
    tableId: id,
    tableName: data.tableName,
  });

  const columnActions = useColumns({
    schemaId: data.schemaId,
    tableId: id,
    columns,
  });

  const indexActions = useIndexes({
    erdStore,
    schemaId: data.schemaId,
    tableId: id,
    tableName: data.tableName,
    indexes,
  });

  const constraintActions = useConstraints({
    erdStore,
    schemaId: data.schemaId,
    tableId: id,
    tableName: data.tableName,
    constraints,
  });

  const dragAndDrop = useDragAndDrop({
    items: columns,
    onReorder: async (draggedColumnId, newIndex) => {
      try {
        await columnService.updateColumnPosition(
          data.schemaId,
          id,
          draggedColumnId,
          newIndex,
        );
      } catch (error) {
        console.error('Failed to update column position:', error);
      }
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
        <div className="p-4 text-center text-schemafy-dark-gray text-sm">
          Click + to add a column.
        </div>
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
      <ConstraintSection
        schemaId={data.schemaId}
        tableId={id}
        constraints={constraints}
        tableColumns={columns.map((col) => ({ id: col.id, name: col.name }))}
        isEditMode={isColumnEditMode}
        onCreateConstraint={constraintActions.createConstraint}
        onDeleteConstraint={constraintActions.deleteConstraint}
        onChangeConstraintName={constraintActions.changeConstraintName}
        onAddColumnToConstraint={constraintActions.addColumnToConstraint}
        onRemoveColumnFromConstraint={
          constraintActions.removeColumnFromConstraint
        }
      />
    </div>
  );
};

export const TableNode = observer(TableNodeComponent);
