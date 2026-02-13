import type { Constraint } from '@/types';
import type { CompositeConstraintKind } from '../hooks/useConstraints';

export interface ConstraintSectionProps {
  schemaId: string;
  tableId: string;
  constraints: Constraint[];
  tableColumns: Array<{ id: string; name: string }>;
  isEditMode: boolean;
  onCreateConstraint: (kind: CompositeConstraintKind) => void;
  onDeleteConstraint: (constraintId: string) => void;
  onChangeConstraintName: (constraintId: string, newName: string) => void;
  onAddColumnToConstraint: (constraintId: string, columnId: string) => void;
  onRemoveColumnFromConstraint: (
    constraintId: string,
    constraintColumnId: string,
  ) => void;
}

export interface ConstraintRowProps {
  constraint: Constraint;
  tableColumns: Array<{ id: string; name: string }>;
  isEditMode: boolean;
  onDeleteConstraint: (constraintId: string) => void;
  onChangeConstraintName: (constraintId: string, newName: string) => void;
  onAddColumnToConstraint: (constraintId: string, columnId: string) => void;
  onRemoveColumnFromConstraint: (
    constraintId: string,
    constraintColumnId: string,
  ) => void;
}

export interface ViewModeConstraintProps {
  constraint: Constraint;
  tableColumns: Array<{ id: string; name: string }>;
}

export interface EditModeConstraintProps {
  constraint: Constraint;
  tableColumns: Array<{ id: string; name: string }>;
  onDeleteConstraint: (constraintId: string) => void;
  onChangeConstraintName: (constraintId: string, newName: string) => void;
  onAddColumnToConstraint: (constraintId: string, columnId: string) => void;
  onRemoveColumnFromConstraint: (
    constraintId: string,
    constraintColumnId: string,
  ) => void;
}
