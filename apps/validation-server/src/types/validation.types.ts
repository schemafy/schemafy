/**
 * Type definitions matching validation.proto
 * These types should be kept in sync with the proto file
 */

import type { Database, Column, Schema, Table } from '@schemafy/validator';

// ============================================================================
// Column RPC Request Types
// ============================================================================

export interface CreateColumnRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  column: Column;
}

export interface DeleteColumnRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  columnId: Column['id'];
}

export interface ChangeColumnNameRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  columnId: Column['id'];
  newName: Column['name'];
}

export interface ChangeColumnTypeRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  columnId: Column['id'];
  dataType: Column['dataType'];
  lengthScale?: Column['lengthScale'];
}

export interface ChangeColumnPositionRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  columnId: Column['id'];
  newPosition: number;
}

export interface ChangeColumnNullableRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  columnId: Column['id'];
  nullable: boolean;
}

// ============================================================================
// Table RPC Request Types
// ============================================================================

export interface CreateTableRequest {
  database: Database;
  schemaId: Schema['id'];
  table: Table;
}

export interface DeleteTableRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
}

export interface ChangeTableNameRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  newName: Table['name'];
}

// ============================================================================
// Schema RPC Request Types
// ============================================================================

export interface CreateSchemaRequest {
  database: Database;
  schema: Schema;
}

export interface DeleteSchemaRequest {
  database: Database;
  schemaId: Schema['id'];
}

// ============================================================================
// Index RPC Request Types
// ============================================================================

export interface CreateIndexRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  index: any; // Replace with proper Index type when available
}

export interface DeleteIndexRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  indexId: string;
}

export interface ChangeIndexNameRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  indexId: string;
  newName: string;
}

export interface AddColumnToIndexRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  indexId: string;
  indexColumn: any; // Replace with proper IndexColumn type when available
}

export interface RemoveColumnFromIndexRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  indexId: string;
  indexColumnId: string;
}

// ============================================================================
// Constraint RPC Request Types
// ============================================================================

export interface CreateConstraintRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  constraint: any; // Replace with proper Constraint type when available
}

export interface DeleteConstraintRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  constraintId: string;
}

export interface ChangeConstraintNameRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  constraintId: string;
  newName: string;
}

export interface AddColumnToConstraintRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  constraintId: string;
  constraintColumn: any; // Replace with proper ConstraintColumn type when available
}

export interface RemoveColumnFromConstraintRequest {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  constraintId: string;
  constraintColumnId: string;
}

// ============================================================================
// Relationship RPC Request Types
// ============================================================================

export interface CreateRelationshipRequest {
  database: Database;
  relationship: any; // Replace with proper Relationship type when available
}

export interface DeleteRelationshipRequest {
  database: Database;
  relationshipId: string;
}

export interface ChangeRelationshipNameRequest {
  database: Database;
  relationshipId: string;
  newName: string;
}

export interface AddColumnPairToRelationshipRequest {
  database: Database;
  relationshipId: string;
  columnPair: any; // Replace with proper ColumnPair type when available
}

export interface RemoveColumnPairFromRelationshipRequest {
  database: Database;
  relationshipId: string;
  columnPairId: string;
}

// ============================================================================
// Validation RPC Request Types
// ============================================================================

export interface ValidateDatabaseRequest {
  database: Database;
}
