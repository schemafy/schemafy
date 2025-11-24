import { Plus } from 'lucide-react';
import { ConstraintRow } from './ConstraintRow';
import type { ConstraintSectionProps } from '../types';

export const ConstraintSection = ({
  constraints,
  tableColumns,
  isEditMode,
  onCreateConstraint,
  onDeleteConstraint,
  onChangeConstraintName,
  onAddColumnToConstraint,
  onRemoveColumnFromConstraint,
}: ConstraintSectionProps) => {
  const filteredConstraints = constraints.filter((constraint) => {
    return constraint.kind === 'UNIQUE';
  });

  if (filteredConstraints.length === 0 && !isEditMode) {
    return null;
  }

  const handleCreateConstraint = () => {
    onCreateConstraint('UNIQUE');
  };

  return (
    <div className="border-t-2 border-schemafy-button-bg">
      <div className="bg-schemafy-dark-gray-40 p-2 flex items-center justify-between">
        <span className="text-xs font-medium text-schemafy-text">
          CONSTRAINTS
        </span>
        {isEditMode && (
          <button
            onClick={handleCreateConstraint}
            className="p-0.5 text-schemafy-text hover:bg-schemafy-dark-gray-40 rounded transition-colors"
            title="Add UNIQUE constraint"
          >
            <Plus size={14} />
          </button>
        )}
      </div>

      <div>
        {filteredConstraints.length === 0 ? (
          <div className="p-2 text-center text-schemafy-dark-gray text-xs">
            No constraints defined
          </div>
        ) : (
          filteredConstraints.map((constraint) => (
            <ConstraintRow
              key={constraint.id}
              constraint={constraint}
              tableColumns={tableColumns}
              isEditMode={isEditMode}
              onDeleteConstraint={onDeleteConstraint}
              onChangeConstraintName={onChangeConstraintName}
              onAddColumnToConstraint={onAddColumnToConstraint}
              onRemoveColumnFromConstraint={onRemoveColumnFromConstraint}
            />
          ))
        )}
      </div>
    </div>
  );
};
