import { useState, useEffect, useMemo, Fragment } from 'react';
import { Trash2, GripVertical } from 'lucide-react';
import { CONSTRAINTS } from '../types';
import type {
  ColumnRowProps,
  EditModeColumnProps,
  ViewModeColumnProps,
  DragHandleProps,
  TypeSelectorProps,
  ColumnConstraintsProps,
  ColumnBadgesProps,
} from '../types';
import type { VendorDatatype } from '../api/vendor.types';
import {
  Select,
  SelectGroup,
  SelectContent,
  SelectItem,
  SelectLabel,
  SelectTrigger,
} from '@/components';

export const ColumnRow = ({
  column,
  isEditMode,
  isLastColumn,
  vendorTypes,
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
          vendorTypes={vendorTypes}
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

const EditModeColumn = ({
  column,
  isLastColumn,
  vendorTypes,
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

  const handleTypeChange = (dataType: string, lengthScale: string) => {
    onUpdateColumn(
      column.id,
      'type',
      JSON.stringify({ dataType, lengthScale }),
    );
  };

  const isFk = column.isForeignKey;
  const isDeleteDisabled = isLastColumn || isFk;
  const deleteTitle = isFk
    ? 'Cannot delete a foreign key column'
    : isLastColumn
      ? 'Cannot delete the last column'
      : 'Remove Column';

  return (
    <div className="p-2 space-y-2 text-schemafy-text">
      <div className="flex items-center gap-2">
        <DragHandle
          columnId={column.id}
          onDragStart={onDragStart}
          onDragEnd={onDragEnd}
        />

        <input
          type="text"
          value={localName}
          onChange={(e) => handleNameChange(e.target.value)}
          className="flex-1 px-2 py-1 text-sm border border-schemafy-light-gray rounded focus:outline-none focus:ring-1 focus:ring-blue-500"
          placeholder="Column name"
        />

        <TypeSelector
          value={column.type}
          lengthScale={column.lengthScale}
          vendorTypes={vendorTypes}
          disabled={isFk}
          onChange={handleTypeChange}
        />

        <button
          onClick={() => onRemoveColumn(column.id)}
          disabled={isDeleteDisabled}
          className={`p-1 rounded flex-shrink-0 ${
            isDeleteDisabled
              ? 'text-schemafy-dark-gray cursor-not-allowed'
              : 'text-schemafy-destructive hover:bg-red-100'
          }`}
          title={deleteTitle}
        >
          <Trash2 size={12} />
        </button>
      </div>

      <ColumnConstraints column={column} onUpdateColumn={onUpdateColumn} />
    </div>
  );
};

const ViewModeColumn = ({ column }: ViewModeColumnProps) => {
  const nameClassName = column.isPrimaryKey
    ? 'font-bold text-schemafy-yellow'
    : column.isForeignKey
      ? 'font-bold text-schemafy-green'
      : 'text-schemafy-text';

  return (
    <div className="p-2 text-schemafy-text">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <span className={`text-sm ${nameClassName}`}>{column.name}</span>
          <span className="text-xs text-schemafy-dark-gray font-mono">
            ({formatTypeDisplay(column.type, column.lengthScale)})
          </span>
        </div>

        <ColumnBadges column={column} />
      </div>
    </div>
  );
};

const DragHandle = ({ columnId, onDragStart, onDragEnd }: DragHandleProps) => {
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

const CATEGORY_LABELS: Record<string, string> = {
  numeric: 'Numeric',
  datetime: 'Date & Time',
  string: 'String',
  binary: 'Binary',
  json: 'JSON',
};

const getCategoryGroup = (category: string): string => {
  const prefix = category.split('_')[0];
  return prefix;
};

const groupTypesByCategory = (
  types: VendorDatatype[],
): Map<string, VendorDatatype[]> => {
  const groups = new Map<string, VendorDatatype[]>();
  for (const type of types) {
    const group = getCategoryGroup(type.category);
    if (!groups.has(group)) {
      groups.set(group, []);
    }
    groups.get(group)!.push(type);
  }
  return groups;
};

const parseLengthScale = (
  lengthScale: string,
): Record<string, number | null> => {
  try {
    return JSON.parse(lengthScale || '{}');
  } catch {
    return {};
  }
};

const formatTypeDisplay = (type: string, lengthScale: string): string => {
  const parsed = parseLengthScale(lengthScale);
  const values = Object.values(parsed).filter((v) => v != null);
  if (values.length === 0) return type;
  return `${type}(${values.join(',')})`;
};

export const TypeSelector = ({
  value,
  lengthScale,
  vendorTypes,
  disabled,
  onChange,
}: TypeSelectorProps) => {
  const parsed = parseLengthScale(lengthScale);
  const selectedType = vendorTypes.find((t) => t.sqlType === value);
  const params = selectedType?.parameters ?? [];
  const grouped = useMemo(
    () => groupTypesByCategory(vendorTypes),
    [vendorTypes],
  );

  const handleTypeSelect = (newType: string) => {
    onChange(newType, '{}');
  };

  const handleParamBlur = (paramName: string, paramValue: string) => {
    const num = Number(paramValue);
    const updated = {
      ...parsed,
      [paramName]: paramValue && !isNaN(num) ? num : null,
    };
    onChange(value, JSON.stringify(updated));
  };

  return (
    <div className="flex items-center gap-0.5 text-xs font-mono">
      <Select
        onValueChange={handleTypeSelect}
        value={value}
        disabled={disabled}
      >
        <SelectTrigger
          className="text-xs font-mono px-2 py-1 border border-schemafy-light-gray rounded focus:outline-none w-auto min-w-[5rem] [&>span]:line-clamp-none"
          title={
            disabled
              ? 'Cannot change the type of a foreign key column'
              : undefined
          }
        >
          <span className="flex items-center gap-0.5 whitespace-nowrap">
            <span>{value || 'Type'}</span>
            {params.length > 0 && (
              <>
                <span>(</span>
                {params
                  .sort((a, b) => a.order - b.order)
                  .map((param, i) => (
                    <Fragment key={param.name}>
                      {i > 0 && <span>,</span>}
                      <input
                        key={`${value}-${param.name}`}
                        type="number"
                        defaultValue={parsed[param.name] ?? ''}
                        placeholder={param.label}
                        onPointerDown={(e) => e.stopPropagation()}
                        onClick={(e) => {
                          e.stopPropagation();
                          e.preventDefault();
                        }}
                        onMouseDown={(e) => e.stopPropagation()}
                        onKeyDown={(e) => {
                          e.stopPropagation();
                          if (e.key === 'Enter') e.currentTarget.blur();
                        }}
                        onBlur={(e) =>
                          handleParamBlur(param.name, e.target.value)
                        }
                        className="w-8 text-center bg-transparent border-b border-schemafy-dark-gray focus:outline-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                      />
                    </Fragment>
                  ))}
                <span>)</span>
              </>
            )}
          </span>
        </SelectTrigger>
        <SelectContent className="max-h-60">
          {[...grouped.entries()].map(([group, types], i) => (
            <SelectGroup
              key={group}
              className={
                i > 0 ? 'border-t border-schemafy-light-gray mt-1 pt-1' : ''
              }
            >
              <SelectLabel>{CATEGORY_LABELS[group] ?? group}</SelectLabel>
              {types.map((type) => (
                <SelectItem key={type.sqlType} value={type.sqlType}>
                  {type.displayName}
                </SelectItem>
              ))}
            </SelectGroup>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
};

const ColumnConstraints = ({
  column,
  onUpdateColumn,
}: ColumnConstraintsProps) => {
  return (
    <div className="flex flex-wrap gap-3 text-xs ml-4">
      {CONSTRAINTS.filter(({ visible }) => visible).map(
        ({ key, label, color }) => {
          const isFkDisabled = column.isForeignKey && key === 'isPrimaryKey';
          const isDisabled =
            isFkDisabled || (column.isPrimaryKey && key === 'isNotNull');
          const title = isFkDisabled
            ? 'Cannot change PK constraint on a foreign key column'
            : undefined;

          return (
            <label
              key={key}
              className={`flex items-center gap-1 ${isDisabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}
              title={title}
            >
              <input
                type="checkbox"
                checked={column[key]}
                onChange={(e) =>
                  onUpdateColumn(column.id, key, e.target.checked)
                }
                disabled={isDisabled}
                className="w-3 h-3"
              />
              <span className={`${color} font-medium`}>{label}</span>
            </label>
          );
        },
      )}
    </div>
  );
};

const ColumnBadges = ({ column }: ColumnBadgesProps) => {
  return (
    <div className="flex items-center gap-1">
      {column.isPrimaryKey && (
        <span className="text-xs text-schemafy-yellow font-medium">PK</span>
      )}
      {column.isForeignKey && (
        <span className="text-xs text-schemafy-green font-medium">FK</span>
      )}
      {!column.isPrimaryKey && column.isNotNull && (
        <span className="text-xs text-schemafy-destructive font-medium">*</span>
      )}
      {!column.isPrimaryKey && column.isUnique && (
        <span className="text-xs text-schemafy-blue font-medium">UQ</span>
      )}
    </div>
  );
};
