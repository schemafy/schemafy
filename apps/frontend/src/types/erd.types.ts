export const SCHEMA_NAME_CONSTRAINTS = {
  MIN_LENGTH: 1,
  MAX_LENGTH: 20,
} as const;

export type DbVendorId = 'MYSQL';
export type IndexType = 'BTREE' | 'HASH' | 'FULLTEXT' | 'SPATIAL' | 'OTHER';
export type IndexSortDir = 'ASC' | 'DESC';
export type ConstraintKind =
  | 'PRIMARY_KEY'
  | 'UNIQUE'
  | 'CHECK'
  | 'DEFAULT'
  | 'NOT_NULL';
export type RelationshipKind = 'IDENTIFYING' | 'NON_IDENTIFYING';
export type RelationshipCardinality = 'ONE_TO_ONE' | 'ONE_TO_MANY';
export type RelationshipAction =
  | 'NO_ACTION'
  | 'RESTRICT'
  | 'CASCADE'
  | 'SET_NULL'
  | 'SET_DEFAULT';

export interface Column {
  id: string;
  tableId: string;
  name: string;
  seqNo: number;
  ordinalPosition: number;
  dataType?: string | null;
  lengthScale: string;
  isAutoIncrement: boolean;
  charset: string;
  collation: string;
  comment?: string | null;
  isAffected: boolean;
}

export interface IndexColumn {
  id: string;
  indexId: string;
  columnId: string;
  seqNo: number;
  sortDir: IndexSortDir;
  isAffected: boolean;
}

export interface Index {
  id: string;
  tableId: string;
  name: string;
  type: IndexType;
  comment?: string | null;
  columns: IndexColumn[];
  isAffected: boolean;
}

export interface ConstraintColumn {
  id: string;
  constraintId: string;
  columnId: string;
  seqNo: number;
  isAffected: boolean;
}

export interface Constraint {
  id: string;
  tableId: string;
  name: string;
  kind: ConstraintKind;
  checkExpr?: string | null;
  defaultExpr?: string | null;
  columns: ConstraintColumn[];
  isAffected: boolean;
}

export interface RelationshipColumn {
  id: string;
  relationshipId: string;
  fkColumnId: string;
  pkColumnId: string;
  seqNo: number;
  isAffected: boolean;
}

export interface Relationship {
  id: string;
  fkTableId: string;
  pkTableId: string;
  name: string;
  kind: RelationshipKind;
  cardinality: RelationshipCardinality;
  onDelete: RelationshipAction;
  onUpdate: RelationshipAction;
  fkEnforced: false;
  columns: RelationshipColumn[];
  isAffected: boolean;
  extra?: unknown;
}

export interface Table {
  id: string;
  schemaId: string;
  name: string;
  comment?: string | null;
  tableOptions: string;
  columns: Column[];
  indexes: Index[];
  constraints: Constraint[];
  relationships: Relationship[];
  isAffected: boolean;
  extra?: unknown;
}

export interface Schema {
  id: string;
  projectId: string;
  dbVendorId: DbVendorId;
  name: string;
  charset: string;
  collation: string;
  vendorOption: string;
  tables: Table[];
  isAffected: boolean;
  extra?: unknown;
}

export interface Database {
  id: string;
  schemas: Schema[];
  isAffected: boolean;
  extra?: unknown;
}
