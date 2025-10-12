import type { DragEvent } from 'react';

export type FieldType = {
  id: string;
  name: string;
  type: string;
  isPrimaryKey: boolean;
  isNotNull: boolean;
  isUnique: boolean;
};

export interface FieldRowProps {
  field: FieldType;
  isEditMode: boolean;
  draggedItem: string | null;
  dragOverItem: string | null;
  onDragStart: (e: DragEvent, fieldId: string) => void;
  onDragOver: (e: DragEvent, fieldId: string) => void;
  onDragLeave: (e: DragEvent) => void;
  onDrop: (e: DragEvent, fieldId: string) => void;
  onDragEnd: () => void;
  onUpdateField: (fieldId: string, key: keyof FieldType, value: string | boolean) => void;
  onRemoveField: (fieldId: string) => void;
}

export interface EditModeFieldProps {
  field: FieldType;
  onDragStart: (e: DragEvent, fieldId: string) => void;
  onDragEnd: () => void;
  onUpdateField: (fieldId: string, key: keyof FieldType, value: string | boolean) => void;
  onRemoveField: (fieldId: string) => void;
}

export interface ViewModeFieldProps {
  field: FieldType;
}

export interface DragHandleProps {
  fieldId: string;
  onDragStart: (e: DragEvent, fieldId: string) => void;
  onDragEnd: () => void;
}

export interface TypeSelectorProps {
  value: string;
  onChange: (value: string) => void;
}

export interface FieldConstraintsProps {
  field: FieldType;
  onUpdateField: (fieldId: string, key: keyof FieldType, value: boolean) => void;
}

export interface FieldBadgesProps {
  field: FieldType;
}

export const DATA_TYPES = [
  'VARCHAR',
  'CHAR',
  'TEXT',
  'LONGTEXT',
  'INT',
  'BIGINT',
  'SMALLINT',
  'TINYINT',
  'DECIMAL',
  'FLOAT',
  'DOUBLE',
  'DATETIME',
  'DATE',
  'TIME',
  'TIMESTAMP',
  'BOOLEAN',
  'JSON',
  'UUID',
];
