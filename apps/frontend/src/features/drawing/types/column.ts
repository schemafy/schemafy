import type { DragEvent } from 'react';

export type ColumnType = {
  id: string;
  name: string;
  type: string;
  isPrimaryKey: boolean;
  isNotNull: boolean;
  isUnique: boolean;
};

export interface ColumnRowProps {
  column: ColumnType;
  isEditMode: boolean;
  isLastColumn: boolean;
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
  onChange: (value: string) => void;
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

// TODO: 데이터 타입은 백엔드에서 length_scale이랑 같이 제공
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

export type ConstraintKind = 'PRIMARY_KEY' | 'NOT_NULL' | 'UNIQUE';

export const CONSTRAINTS = [
  { key: 'isPrimaryKey', label: 'PK', color: 'text-schemafy-yellow' },
  { key: 'isNotNull', label: 'NOT NULL', color: 'text-schemafy-destructive' },
] as const;
