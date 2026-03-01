import type { DragEvent } from 'react';
import type { VendorDatatype } from '../api';

export type ColumnType = {
  id: string;
  name: string;
  type: string;
  lengthScale: string;
  isPrimaryKey: boolean;
  isForeignKey: boolean;
  isNotNull: boolean;
  isUnique: boolean;
};

export interface ColumnRowProps {
  column: ColumnType;
  isEditMode: boolean;
  isLastColumn: boolean;
  vendorTypes: VendorDatatype[];
  draggedItem: string | null;
  dragOverItem: string | null;
  onDragStart: (e: DragEvent, columnId: string) => void;
  onDragOver: (e: DragEvent, columnId: string) => void;
  onDragLeave: (e: DragEvent) => void;
  onDrop: (e: DragEvent, columnId: string) => void;
  onDragEnd: () => void;
  onUpdateColumn: (
    columnId: string,
    key: keyof ColumnType,
    value: string | boolean,
  ) => void;
  onRemoveColumn: (columnId: string) => void;
}

export interface EditModeColumnProps {
  column: ColumnType;
  isLastColumn: boolean;
  vendorTypes: VendorDatatype[];
  onDragStart: (e: DragEvent, columnId: string) => void;
  onDragEnd: () => void;
  onUpdateColumn: (
    columnId: string,
    key: keyof ColumnType,
    value: string | boolean,
  ) => void;
  onRemoveColumn: (columnId: string) => void;
}

export interface ViewModeColumnProps {
  column: ColumnType;
}

export interface DragHandleProps {
  columnId: string;
  onDragStart: (e: DragEvent, columnId: string) => void;
  onDragEnd: () => void;
}

export interface TypeSelectorProps {
  value: string;
  lengthScale: string;
  vendorTypes: VendorDatatype[];
  disabled?: boolean;
  onChange: (dataType: string, lengthScale: string) => void;
}

export interface ColumnConstraintsProps {
  column: ColumnType;
  onUpdateColumn: (
    columnId: string,
    key: keyof ColumnType,
    value: boolean,
  ) => void;
}

export interface ColumnBadgesProps {
  column: ColumnType;
}

export const CONSTRAINTS = [
  {
    kind: 'PRIMARY_KEY' as const,
    key: 'isPrimaryKey' as const,
    label: 'PK',
    color: 'text-schemafy-yellow',
    visible: true,
  },
  {
    kind: 'NOT_NULL' as const,
    key: 'isNotNull' as const,
    label: 'NOT NULL',
    color: 'text-schemafy-destructive',
    visible: true,
  },
  {
    kind: 'UNIQUE' as const,
    key: 'isUnique' as const,
    label: 'UNIQUE',
    color: 'text-schemafy-blue',
    visible: false,
  },
] as const;

export type ConstraintKind = (typeof CONSTRAINTS)[number]['kind'];
