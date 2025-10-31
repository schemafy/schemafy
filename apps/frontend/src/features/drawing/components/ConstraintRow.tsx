import { useState, useEffect } from 'react';
import { Trash2, X } from 'lucide-react';
import type { ConstraintRowProps, ViewModeConstraintProps, EditModeConstraintProps } from '../types';
import { Select, SelectGroup, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components';

export const ConstraintRow = ({
  constraint,
  tableColumns,
  isEditMode,
  onDeleteConstraint,
  onChangeConstraintName,
  onAddColumnToConstraint,
  onRemoveColumnFromConstraint,
}: ConstraintRowProps) => {
  return (
    <div className="border-b border-schemafy-light-gray last:border-b-0">
      {isEditMode ? (
        <EditModeConstraint
          constraint={constraint}
          tableColumns={tableColumns}
          onDeleteConstraint={onDeleteConstraint}
          onChangeConstraintName={onChangeConstraintName}
          onAddColumnToConstraint={onAddColumnToConstraint}
          onRemoveColumnFromConstraint={onRemoveColumnFromConstraint}
        />
      ) : (
        <ViewModeConstraint constraint={constraint} tableColumns={tableColumns} />
      )}
    </div>
  );
};

export const ViewModeConstraint = ({ constraint, tableColumns }: ViewModeConstraintProps) => {
  const getColumnName = (columnId: string) => {
    return tableColumns.find((col) => col.id === columnId)?.name || 'Unknown';
  };

  const columnsStr = constraint.columns
    .sort((a, b) => a.seqNo - b.seqNo)
    .map((col) => getColumnName(col.columnId))
    .join(', ');

  const kindLabel = constraint.kind === 'PRIMARY_KEY' ? 'PK' : 'UNIQUE';

  return (
    <div className="p-2">
      <div className="text-xs text-schemafy-dark-gray font-mono">
        <span className="text-schemafy-text font-medium">{constraint.name}</span>{' '}
        <span className="text-schemafy-purple">{kindLabel}</span>
        {columnsStr && (
          <>
            {' '}
            (<span className="text-schemafy-blue">{columnsStr}</span>)
          </>
        )}
      </div>
    </div>
  );
};

export const EditModeConstraint = ({
  constraint,
  tableColumns,
  onDeleteConstraint,
  onChangeConstraintName,
  onAddColumnToConstraint,
  onRemoveColumnFromConstraint,
}: EditModeConstraintProps) => {
  const [localName, setLocalName] = useState(constraint.name);

  useEffect(() => {
    setLocalName(constraint.name);
  }, [constraint.name]);

  const handleNameChange = (value: string) => {
    setLocalName(value);
    onChangeConstraintName(constraint.id, value);
  };

  const getColumnName = (columnId: string) => {
    return tableColumns.find((col) => col.id === columnId)?.name || 'Unknown';
  };

  const availableColumns = tableColumns.filter((col) => !constraint.columns.some((cCol) => cCol.columnId === col.id));

  const kindLabel = constraint.kind === 'PRIMARY_KEY' ? 'PK' : 'UNIQUE';

  return (
    <div className="p-2 space-y-2 bg-schemafy-bg text-schemafy-text">
      <div className="flex items-center gap-2">
        <input
          type="text"
          value={localName}
          onChange={(e) => handleNameChange(e.target.value)}
          className="flex-1 px-2 py-1 text-sm border border-schemafy-light-gray rounded focus:outline-none"
          placeholder="Constraint name"
        />
        <span className="text-xs font-mono text-schemafy-dark-gray">{kindLabel}</span>

        <button
          onClick={() => onDeleteConstraint(constraint.id)}
          className="rounded flex-shrink-0 text-schemafy-destructive hover:bg-red-100"
          title="Remove Constraint"
        >
          <Trash2 size={12} />
        </button>
      </div>

      <div className="ml-4 space-y-1">
        {constraint.columns
          .sort((a, b) => a.seqNo - b.seqNo)
          .map((constraintColumn) => (
            <div key={constraintColumn.id} className="flex items-center gap-2 text-xs">
              <span className="text-schemafy-blue font-medium">{getColumnName(constraintColumn.columnId)}</span>

              <button
                onClick={() => onRemoveColumnFromConstraint(constraint.id, constraintColumn.id)}
                className="p-0.5 rounded"
                title="Remove column from constraint"
              >
                <X size={12} />
              </button>
            </div>
          ))}

        {availableColumns.length > 0 && (
          <div className="flex items-center gap-2">
            <Select
              onValueChange={(value) => {
                if (value) {
                  onAddColumnToConstraint(constraint.id, value);
                  value = '';
                }
              }}
              value=""
            >
              <SelectTrigger className="w-[8rem] text-xs font-mono py-1 px-1.5 border border-schemafy-light-gray rounded focus:outline-none">
                <SelectValue placeholder="+ Add column" />
              </SelectTrigger>
              <SelectContent popover="auto">
                <SelectGroup>
                  {availableColumns.map((col) => (
                    <SelectItem value={col.id} key={col.id}>
                      {col.name}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>
        )}
      </div>
    </div>
  );
};
