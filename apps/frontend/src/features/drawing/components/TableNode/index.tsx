import { useState } from 'react';
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
  useChangeColumnPosition,
} from '../../hooks';
import { ConnectionHandles } from './ConnectionHandles';
import { useVendors, useVendor } from '../../api/vendor.hooks';
import type { VendorDatatype } from '../../api/vendor.types';

const TableNodeComponent = ({ data, id }: TableProps) => {
  const [isColumnEditMode, setIsColumnEditMode] = useState(false);

  const schema =
    erdStore.erdState.state === 'loaded'
      ? erdStore.erdState.database.schemas.find((s) => s.id === data.schemaId)
      : undefined;

  const { data: vendorsData } = useVendors();
  const vendorDisplayName =
    schema && vendorsData?.result
      ? (vendorsData.result.find(
          (v) => v.name === schema.dbVendorId.toLowerCase(),
        )?.displayName ?? '')
      : '';

  const { data: vendorData } = useVendor(vendorDisplayName);
  const vendorTypes: VendorDatatype[] =
    vendorData?.result?.datatypeMappings?.types ?? [];

  const { columns, indexes, constraints } = data;

  const { updateColumn, saveAllPendingChanges: saveColumnAllPendingChanges } =
    useColumn(data.schemaId, id, data.tableName, constraints);

  const changeColumnPositionMutation = useChangeColumnPosition(data.schemaId);

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
    schemaId: data.schemaId,
    tableId: id,
    tableName: data.tableName,
    indexes,
  });

  const constraintActions = useConstraints({
    schemaId: data.schemaId,
    tableId: id,
    tableName: data.tableName,
    constraints,
  });

  const dragAndDrop = useDragAndDrop({
    items: columns,
    onReorder: (_newColumns, draggedColumnId, newIndex) => {
      changeColumnPositionMutation.mutate({
        columnId: draggedColumnId,
        data: { seqNo: newIndex },
      });
    },
  });

  const handleAddColumn = () => {
    columnActions.addColumn();
    setIsColumnEditMode(true);
  };

  const handleSaveAllPendingChanges = () => {
    saveColumnAllPendingChanges();
    indexActions.saveAllPendingChanges();
    constraintActions.saveAllPendingChanges();
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
        isAddingColumn={columnActions.isAddingColumn}
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
            vendorTypes={vendorTypes}
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
        onUpdateIndexName={indexActions.updateIndexName}
        onUpdateIndexType={indexActions.updateIndexType}
        onAddColumnToIndex={indexActions.addColumnToIndex}
        onRemoveColumnFromIndex={indexActions.removeColumnFromIndex}
        onUpdateSortDir={indexActions.updateSortDir}
      />
      <ConstraintSection
        schemaId={data.schemaId}
        tableId={id}
        constraints={constraints}
        tableColumns={columns.map((col) => ({ id: col.id, name: col.name }))}
        isEditMode={isColumnEditMode}
        onCreateConstraint={constraintActions.createConstraint}
        onDeleteConstraint={constraintActions.deleteConstraint}
        onUpdateConstraintName={constraintActions.updateConstraintName}
        onAddColumnToConstraint={constraintActions.addColumnToConstraint}
        onRemoveColumnFromConstraint={
          constraintActions.removeColumnFromConstraint
        }
      />
    </div>
  );
};

export const TableNode = TableNodeComponent;
