import { Handle, Position } from '@xyflow/react';
import { useState } from 'react';
import { HANDLE_STYLE, type TableProps, type ColumnType } from '../types';
import { Edit, Check, Settings, Plus } from 'lucide-react';
import { ColumnRow } from './Column';
import { useDragAndDrop } from '../hooks';

export const TableNode = ({ data, id }: TableProps) => {
  const [isEditingTableName, setIsEditingTableName] = useState(false);
  const [isColumnEditMode, setIsColumnEditMode] = useState(false);
  const [editingTableName, setEditingTableName] = useState(data.tableName);
  const [columns, setColumns] = useState(data.columns || []);

  const dragAndDrop = useDragAndDrop({
    items: columns,
    onReorder: (newColumns) => {
      setColumns(newColumns);
      updateColumns(newColumns);
    },
  });

  const saveTableName = () => {
    if (data.updateTable) {
      data.updateTable(id, { tableName: editingTableName });
    }
    setIsEditingTableName(false);
  };

  const addColumn = () => {
    const newColumn: ColumnType = {
      id: `column_${Date.now()}`,
      name: 'new_column',
      type: 'VARCHAR',
      isPrimaryKey: false,
      isNotNull: false,
      isUnique: false,
    };
    const newColumns = [...columns, newColumn];
    setColumns(newColumns);
    updateColumns(newColumns);
    setIsColumnEditMode(true);
  };

  const removeColumn = (columnId: string) => {
    const newColumns = columns.filter((column) => column.id !== columnId);
    setColumns(newColumns);
    updateColumns(newColumns);
  };

  const updateColumn = (columnId: string, key: keyof ColumnType, value: string | boolean) => {
    const newColumns = columns.map((column) => (column.id === columnId ? { ...column, [key]: value } : column));
    setColumns(newColumns);
    updateColumns(newColumns);
  };

  const updateColumns = (newColumns: ColumnType[]) => {
    if (data.updateTable) {
      data.updateTable(id, { columns: newColumns });
    }
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
        onAddColumn={addColumn}
      />
      <div className="max-h-96 overflow-y-auto">
        {columns.map((column) => (
          <ColumnRow
            key={column.id}
            column={column}
            isEditMode={isColumnEditMode}
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
    </div>
  );
};

const TableHeader = ({
  tableName,
  isEditing,
  editingName,
  isColumnEditMode,
  onStartEdit,
  onSaveEdit,
  onCancelEdit,
  onEditingNameChange,
  onToggleColumnEditMode,
  onAddColumn,
}: {
  tableName: string;
  isEditing: boolean;
  editingName: string;
  isColumnEditMode: boolean;
  onStartEdit: () => void;
  onSaveEdit: () => void;
  onCancelEdit: () => void;
  onEditingNameChange: (name: string) => void;
  onToggleColumnEditMode: () => void;
  onAddColumn: () => void;
}) => {
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') onSaveEdit();
    if (e.key === 'Escape') onCancelEdit();
  };

  return (
    <div className="bg-schemafy-button-bg text-schemafy-button-text p-3 flex items-center justify-between">
      <div className="flex items-center gap-2 flex-1">
        {isEditing ? (
          <div className="flex items-center gap-2 flex-1">
            <input
              type="text"
              value={editingName}
              placeholder="Entity"
              onChange={(e) => onEditingNameChange(e.target.value)}
              className="bg-transparent border-b border-schemafy-button-text text-schemafy-button-text placeholder-schemafy-dark-gray outline-none flex-1"
              onKeyDown={handleKeyDown}
              autoFocus
            />
            <button onClick={onSaveEdit} className="p-1 hover:bg-schemafy-dark-gray rounded">
              <Check size={14} />
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2 flex-1">
            <span className="font-medium">{tableName}</span>
            <button onClick={onStartEdit} className="p-1 hover:bg-schemafy-dark-gray rounded">
              <Edit size={14} />
            </button>
          </div>
        )}
      </div>

      <div className="flex items-center gap-1">
        <button
          onClick={onToggleColumnEditMode}
          className={`p-1 rounded ${isColumnEditMode ? 'bg-schemafy-dark-gray' : 'hover:bg-schemafy-dark-gray'}`}
          title="Toggle Edit Mode"
        >
          <Settings size={14} />
        </button>
        <button onClick={onAddColumn} className="p-1 hover:bg-schemafy-dark-gray rounded" title="Add Column">
          <Plus size={14} />
        </button>
      </div>
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
