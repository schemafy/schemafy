import { Trash2, GripVertical } from 'lucide-react';
import { DATA_TYPES } from '../types';
import type {
  FieldRowProps,
  EditModeFieldProps,
  ViewModeFieldProps,
  DragHandleProps,
  TypeSelectorProps,
  FieldConstraintsProps,
  FieldBadgesProps,
} from '../types';

export const FieldRow = ({
  field,
  isEditMode,
  draggedItem,
  dragOverItem,
  onDragStart,
  onDragOver,
  onDragLeave,
  onDrop,
  onDragEnd,
  onUpdateField,
  onRemoveField,
}: FieldRowProps) => {
  const rowClassName = `
    border-b border-schemafy-light-gray last:border-b-0 transition-colors duration-200 
    ${isEditMode ? 'hover:bg-schemafy-secondary' : ''}
    ${draggedItem === field.id ? 'opacity-50' : ''}
    ${dragOverItem === field.id ? 'bg-blue-50 border-blue-200' : ''}
  `.trim();

  return (
    <div
      className={rowClassName}
      onDragOver={(e) => onDragOver(e, field.id)}
      onDragLeave={onDragLeave}
      onDrop={(e) => onDrop(e, field.id)}
    >
      {isEditMode ? (
        <EditModeField
          field={field}
          onDragStart={onDragStart}
          onDragEnd={onDragEnd}
          onUpdateField={onUpdateField}
          onRemoveField={onRemoveField}
        />
      ) : (
        <ViewModeField field={field} />
      )}
    </div>
  );
};

export const EditModeField = ({
  field,
  onDragStart,
  onDragEnd,
  onUpdateField,
  onRemoveField,
}: EditModeFieldProps) => {
  return (
    <div className="p-2 space-y-2">
      <div className="flex items-center gap-2">
        <DragHandle
          fieldId={field.id}
          onDragStart={onDragStart}
          onDragEnd={onDragEnd}
        />

        <input
          type="text"
          value={field.name}
          onChange={(e) => onUpdateField(field.id, 'name', e.target.value)}
          className="flex-1 px-2 py-1 text-sm border border-schemafy-light-gray rounded focus:outline-none focus:ring-1 focus:ring-blue-500"
          placeholder="Field name"
        />

        <TypeSelector
          value={field.type}
          onChange={(value) => onUpdateField(field.id, 'type', value)}
        />

        <button
          onClick={() => onRemoveField(field.id)}
          className="p-1 text-schemafy-destructive hover:bg-red-100 rounded flex-shrink-0"
          title="Remove Field"
        >
          <Trash2 size={12} />
        </button>
      </div>

      <FieldConstraints field={field} onUpdateField={onUpdateField} />
    </div>
  );
};

export const ViewModeField = ({ field }: ViewModeFieldProps) => {
  return (
    <div className="p-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span
            className={`text-sm ${
              field.isPrimaryKey
                ? 'font-bold text-yellow-600'
                : 'text-schemafy-text'
            }`}
          >
            {field.name}
          </span>
          <span className="text-xs text-schemafy-dark-gray">
            ({field.type})
          </span>
        </div>

        <FieldBadges field={field} />
      </div>
    </div>
  );
};

export const DragHandle = ({
  fieldId,
  onDragStart,
  onDragEnd,
}: DragHandleProps) => {
  return (
    <span
      draggable
      onDragStart={(e) => onDragStart(e, fieldId)}
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

export const FieldConstraints = ({
  field,
  onUpdateField,
}: FieldConstraintsProps) => {
  const constraints = [
    { key: 'isPrimaryKey', label: 'PK', color: 'text-yellow-600' },
    { key: 'isNotNull', label: 'NOT NULL', color: 'text-red-600' },
    { key: 'isUnique', label: 'UNIQUE', color: 'text-blue-600' },
  ] as const;

  return (
    <div className="flex flex-wrap gap-3 text-xs ml-4">
      {constraints.map(({ key, label, color }) => (
        <label key={key} className="flex items-center gap-1 cursor-pointer">
          <input
            type="checkbox"
            checked={field[key]}
            onChange={(e) => onUpdateField(field.id, key, e.target.checked)}
            className="w-3 h-3"
          />
          <span className={`${color} font-medium`}>{label}</span>
        </label>
      ))}
    </div>
  );
};

export const FieldBadges = ({ field }: FieldBadgesProps) => {
  return (
    <div className="flex items-center gap-1">
      {field.isPrimaryKey && (
        <span className="text-xs text-yellow-600 font-medium">PK</span>
      )}
      {field.isNotNull && (
        <span className="text-xs text-red-600 font-medium">*</span>
      )}
      {field.isUnique && (
        <span className="text-xs text-blue-600 font-medium">UQ</span>
      )}
    </div>
  );
};
