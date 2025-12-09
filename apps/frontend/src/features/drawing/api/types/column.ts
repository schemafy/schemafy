import type { ULID } from './common';

export interface ColumnResponse {
  id: ULID;
  tableId: ULID;
  name: string;
  dataType: string;
  ordinalPosition: number;
  lengthScale: string | null;
  isAutoIncrement: boolean;
  charset: string | null;
  collation: string | null;
  comment: string | null;
}

export interface CreateColumnRequest {
  tableId: ULID;
  name: string;
  dataType: string;
  ordinalPosition: number;
  lengthScale?: string;
  isAutoIncrement?: boolean;
  charset?: string;
  collation?: string;
  comment?: string;
}

export interface UpdateColumnNameRequest {
  columnId: ULID;
  name: string;
}

export interface UpdateColumnTypeRequest {
  columnId: ULID;
  dataType: string;
  lengthScale?: string;
}

export interface UpdateColumnPositionRequest {
  columnId: ULID;
  ordinalPosition: number;
}

export interface DeleteColumnRequest {
  columnId: ULID;
}
