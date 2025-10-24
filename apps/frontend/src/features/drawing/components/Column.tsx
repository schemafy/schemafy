import { useState, useEffect } from 'react';
import { Trash2, GripVertical } from 'lucide-react';
import { DATA_TYPES } from '../types';
import type {
  ColumnRowProps,
  EditModeColumnProps,
  ViewModeColumnProps,
  DragHandleProps,
  TypeSelectorProps,
  ColumnConstraintsProps,
  ColumnBadgesProps,
} from '../types';

export const ColumnRow = ({
  column,
  isEditMode,
  isLastColumn,
  draggedItem,
  dragOverItem,
  onDragStart,
  onDragOver,
  onDragLeave,
  onDrop,
  onDragEnd,
  onUpdateColumn,
  onRemoveColumn,
}: ColumnRowProps) => {
  const rowClassName = `
    border-b border-schemafy-light-gray last:border-b-0 transition-colors duration-200
    ${isEditMode ? 'hover:bg-schemafy-secondary' : ''}
    ${draggedItem === column.id ? 'opacity-50' : ''}
    ${dragOverItem === column.id ? 'bg-blue-50 border-blue-200' : ''}
  `.trim();

  return (
    <div
      className={rowClassName}
      onDragOver={(e) => onDragOver(e, column.id)}
      onDragLeave={onDragLeave}
      onDrop={(e) => onDrop(e, column.id)}
    >
      {isEditMode ? (
        <EditModeColumn
          column={column}
          isLastColumn={isLastColumn}
          onDragStart={onDragStart}
          onDragEnd={onDragEnd}
          onUpdateColumn={onUpdateColumn}
          onRemoveColumn={onRemoveColumn}
        />
      ) : (
        <ViewModeColumn column={column} />
      )}
    </div>
  );
};

export const EditModeColumn = ({
  column,
  isLastColumn,
  onDragStart,
  onDragEnd,
  onUpdateColumn,
  onRemoveColumn,
}: EditModeColumnProps) => {
  const [localName, setLocalName] = useState(column.name);

  useEffect(() => {
    setLocalName(column.name);
  }, [column.name]);

  const handleNameChange = (value: string) => {
    setLocalName(value);
    onUpdateColumn(column.id, 'name', value);
  };

  return (
    <div className="p-2 space-y-2">
      <div className="flex items-center gap-2">
        <DragHandle columnId={column.id} onDragStart={onDragStart} onDragEnd={onDragEnd} />

        <input
          type="text"
          value={localName}
          onChange={(e) => handleNameChange(e.target.value)}
          className="flex-1 px-2 py-1 text-sm border border-schemafy-light-gray rounded focus:outline-none focus:ring-1 focus:ring-blue-500"
          placeholder="Column name"
        />

        <TypeSelector value={column.type} onChange={(value) => onUpdateColumn(column.id, 'type', value)} />

        <button
          onClick={() => onRemoveColumn(column.id)}
          disabled={isLastColumn}
          className={`p-1 rounded flex-shrink-0 ${
            isLastColumn ? 'text-gray-300 cursor-not-allowed' : 'text-schemafy-destructive hover:bg-red-100'
          }`}
          title={isLastColumn ? 'Cannot delete the last column' : 'Remove Column'}
        >
          <Trash2 size={12} />
        </button>
      </div>

      <ColumnConstraints column={column} onUpdateColumn={onUpdateColumn} />
    </div>
  );
};

export const ViewModeColumn = ({ column }: ViewModeColumnProps) => {
  return (
    <div className="p-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className={`text-sm ${column.isPrimaryKey ? 'font-bold text-yellow-600' : 'text-schemafy-text'}`}>
            {column.name}
          </span>
          <span className="text-xs text-schemafy-dark-gray">({column.type})</span>
        </div>

        <ColumnBadges column={column} />
      </div>
    </div>
  );
};

export const DragHandle = ({ columnId, onDragStart, onDragEnd }: DragHandleProps) => {
  return (
    <span
      draggable
      onDragStart={(e) => onDragStart(e, columnId)}
      onDragEnd={onDragEnd}
      className="cursor-move p-1 hover:bg-schemafy-light-gray rounded transition-colors nodrag"
      title="Drag to reorder"
      onMouseDown={(e) => e.stopPropagation()}
    >
      <GripVertical size={12} className="text-schemafy-dark-gray" />
    </span>
  );
};

export const TypeSelector = ({ value, onChange }: TypeSelectorProps) => {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="px-2 py-1 text-sm border border-schemafy-light-gray rounded focus:outline-none focus:ring-1 focus:ring-blue-500"
    >
      {DATA_TYPES.map((type) => (
        <option key={type} value={type}>
          {type}
        </option>
      ))}
    </select>
  );
};

export const ColumnConstraints = ({ column, onUpdateColumn }: ColumnConstraintsProps) => {
  const constraints = [
    { key: 'isPrimaryKey', label: 'PK', color: 'text-yellow-600' },
    { key: 'isNotNull', label: 'NOT NULL', color: 'text-red-600' },
    { key: 'isUnique', label: 'UNIQUE', color: 'text-blue-600' },
  ] as const;

  return (
    <div className="flex flex-wrap gap-3 text-xs ml-4">
      {constraints.map(({ key, label, color }) => {
        const isDisabled = column.isPrimaryKey && (key === 'isNotNull' || key === 'isUnique');

        return (
          <label
            key={key}
            className={`flex items-center gap-1 ${isDisabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}
          >
            <input
              type="checkbox"
              checked={column[key]}
              onChange={(e) => onUpdateColumn(column.id, key, e.target.checked)}
              disabled={isDisabled}
              className="w-3 h-3"
            />
            <span className={`${color} font-medium`}>{label}</span>
          </label>
        );
      })}
    </div>
  );
};

export const ColumnBadges = ({ column }: ColumnBadgesProps) => {
  return (
    <div className="flex items-center gap-1">
      {column.isPrimaryKey && <span className="text-xs text-yellow-600 font-medium">PK</span>}
      {column.isNotNull && <span className="text-xs text-red-600 font-medium">*</span>}
      {column.isUnique && <span className="text-xs text-blue-600 font-medium">UQ</span>}
    </div>
  );
};
