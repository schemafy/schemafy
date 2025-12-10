import type { ColumnResponse } from './column';
import type { ConstraintResponse } from './constraint';
import type { IndexResponse } from './index';
import type { RelationshipResponse } from './relationship';

export type ISODateString = string;
export type ULID = string;
export type IdMapping = Record<ULID, ULID>;
export type NestedIdMapping = Record<ULID, IdMapping>;

export interface AffectedMappingResponse {
  schemas: IdMapping;
  tables: IdMapping;
  columns: NestedIdMapping;
  indexes: NestedIdMapping;
  indexColumns: NestedIdMapping;
  constraints: NestedIdMapping;
  constraintColumns: NestedIdMapping;
  relationships: NestedIdMapping;
  relationshipColumns: NestedIdMapping;
  propagated: PropagatedEntities;
}

export interface PropagatedEntities {
  columns: PropagatedColumn[];
  constraintColumns: PropagatedConstraintColumn[];
  indexColumns: PropagatedIndexColumn[];
}

export interface PropagatedColumn {
  columnId: string;
  tableId: string;
  sourceType: string;
  sourceId: string;
  sourceColumnId: string;
}

export interface PropagatedConstraintColumn {
  constraintColumnId: string;
  constraintId: string;
  columnId: string;
  sourceType: string;
  sourceId: string;
}

export interface PropagatedIndexColumn {
  indexColumnId: string;
  indexId: string;
  columnId: string;
  sourceType: string;
  sourceId: string;
}

export interface DatabaseContext {
  id: ULID;
  schemas: {
    id: ULID;
    projectId: ULID;
    dbVendorId: string;
    name: string;
    charset: string | null;
    collation: string | null;
    vendorOption: string | null;
    tables?: {
      id: ULID;
      schemaId: ULID;
      name: string;
      comment: string | null;
      tableOptions: string | null;
      columns?: ColumnResponse[];
      relationships?: RelationshipResponse[];
      constraints?: ConstraintResponse[];
      indexes?: IndexResponse[];
    }[];
  }[];
}
