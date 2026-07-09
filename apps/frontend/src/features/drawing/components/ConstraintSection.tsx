import { Plus } from 'lucide-react';
import { ConstraintRow } from './ConstraintRow';
import type { ConstraintSectionProps } from '../types';

export const ConstraintSection = ({
  constraints,
  tableColumns,
  isEditMode,
  onCreateConstraint,
  onDeleteConstraint,
  onUpdateConstraintName,
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
    <div className="border-t border-schemafy-glass-border/55">
      <div className="flex items-center justify-between bg-schemafy-secondary/35 px-3 py-1.5">
        <span className="font-overline-xs text-schemafy-dark-gray">
          CONSTRAINTS
        </span>
        {isEditMode && (
          <button
            type="button"
            onClick={handleCreateConstraint}
            className="schemafy-focus-ring flex h-7 w-7 items-center justify-center rounded-lg text-schemafy-dark-gray transition-colors hover:bg-schemafy-secondary hover:text-schemafy-text"
            title="Add UNIQUE constraint"
          >
            <Plus size={14} />
          </button>
        )}
      </div>

      <div>
        {filteredConstraints.length === 0 ? (
          <div className="px-3 py-2.5 text-center text-xs text-schemafy-dark-gray">
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
              onUpdateConstraintName={onUpdateConstraintName}
              onAddColumnToConstraint={onAddColumnToConstraint}
              onRemoveColumnFromConstraint={onRemoveColumnFromConstraint}
            />
          ))
        )}
      </div>
    </div>
  );
};
