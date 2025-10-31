import { Plus, ChevronDown } from 'lucide-react';
import { ConstraintRow } from './ConstraintRow';
import type { ConstraintSectionProps } from '../types';
import { Select, SelectGroup, SelectContent, SelectItem, SelectTrigger } from '@/components';
import type { CompositeConstraintKind } from '../hooks/useConstraints';

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
  if (constraints.length === 0 && !isEditMode) {
    return null;
  }

  const handleCreateConstraint = (kind: CompositeConstraintKind) => {
    onCreateConstraint(kind);
  };

  return (
    <div className="border-t-2 border-schemafy-button-bg">
      <div className="bg-schemafy-dark-gray-40 p-2 flex items-center justify-between">
        <span className="text-xs font-medium text-schemafy-text">CONSTRAINTS</span>
        {isEditMode && (
          <Select onValueChange={(value) => handleCreateConstraint(value as CompositeConstraintKind)}>
            <SelectTrigger className="w-auto h-auto p-0.5 border-0 bg-transparent text-schemafy-text hover:bg-schemafy-dark-gray-40 rounded transition-colors [&>svg]:hidden">
              <div className="flex items-center gap-0.5">
                <Plus size={14} />
                <ChevronDown size={10} />
              </div>
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                <SelectItem value="UNIQUE">UNIQUE</SelectItem>
                <SelectItem value="PRIMARY_KEY">PRIMARY KEY</SelectItem>
              </SelectGroup>
            </SelectContent>
          </Select>
        )}
      </div>

      <div>
        {constraints.length === 0 ? (
          <div className="p-2 text-center text-schemafy-dark-gray text-xs">No constraints defined</div>
        ) : (
          constraints.map((constraint) => (
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
