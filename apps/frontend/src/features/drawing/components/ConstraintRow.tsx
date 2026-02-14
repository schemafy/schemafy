import type {
  ConstraintRowProps,
  ViewModeConstraintProps,
  EditModeConstraintProps,
} from '../types';
import {
  EditableRowBase,
  EditableNameInput,
  DeleteButton,
  ColumnItem,
  AddColumnSelector,
} from './EditableRowBase';
import { getColumnName } from '../utils/columnUtils';

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
    <EditableRowBase
      item={constraint}
      tableColumns={tableColumns}
      isEditMode={isEditMode}
      renderViewMode={(item, cols) => (
        <ViewModeConstraint constraint={item} tableColumns={cols} />
      )}
      renderEditMode={(item, cols) => (
        <EditModeConstraint
          constraint={item}
          tableColumns={cols}
          onDeleteConstraint={onDeleteConstraint}
          onChangeConstraintName={onChangeConstraintName}
          onAddColumnToConstraint={onAddColumnToConstraint}
          onRemoveColumnFromConstraint={onRemoveColumnFromConstraint}
        />
      )}
    />
  );
};

export const ViewModeConstraint = ({
  constraint,
  tableColumns,
}: ViewModeConstraintProps) => {
  const columnsStr = constraint.columns
    .sort((a, b) => a.seqNo - b.seqNo)
    .map((col) => getColumnName(tableColumns, col.columnId))
    .join(', ');

  const kindLabel = constraint.kind === 'PRIMARY_KEY' ? 'PK' : 'UNIQUE';

  return (
    <div className="p-2">
      <div className="text-xs text-schemafy-dark-gray font-mono">
        <span className="text-schemafy-text font-medium">
          {constraint.name}
        </span>{' '}
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
  const availableColumns = tableColumns.filter(
    (col) => !constraint.columns.some((cCol) => cCol.columnId === col.id),
  );

  const kindLabel = constraint.kind === 'PRIMARY_KEY' ? 'PK' : 'UNIQUE';

  return (
    <div className="p-2 space-y-2 bg-schemafy-bg text-schemafy-text">
      <div className="flex items-center gap-2">
        <EditableNameInput
          name={constraint.name}
          placeholder="Constraint name"
          onNameChange={(newName) =>
            onChangeConstraintName(constraint.id, newName)
          }
        />
        <span className="text-xs font-mono text-schemafy-dark-gray">
          {kindLabel}
        </span>

        <DeleteButton
          onDelete={() => onDeleteConstraint(constraint.id)}
          title="Remove Constraint"
        />
      </div>

      <div className="ml-4 space-y-1">
        {constraint.columns
          .sort((a, b) => a.seqNo - b.seqNo)
          .map((constraintColumn) => (
            <ColumnItem
              key={constraintColumn.id}
              columnName={getColumnName(
                tableColumns,
                constraintColumn.columnId,
              )}
              onRemove={() =>
                onRemoveColumnFromConstraint(constraintColumn.id)
              }
            />
          ))}

        <AddColumnSelector
          availableColumns={availableColumns}
          onAddColumn={(columnId) =>
            onAddColumnToConstraint(constraint.id, columnId)
          }
        />
      </div>
    </div>
  );
};
