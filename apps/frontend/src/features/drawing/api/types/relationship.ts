import type { ULID } from './common';

export interface RelationshipColumnResponse {
  id: ULID;
  relationshipId: ULID;
  srcColumnId: ULID;
  tgtColumnId: ULID;
  seqNo: number;
}

export interface RelationshipResponse {
  id: ULID;
  srcTableId: ULID;
  tgtTableId: ULID;
  name: string;
  kind: string;
  cardinality: string;
  onDelete: string | null;
  onUpdate: string | null;
  extra: string | null;
  columns: RelationshipColumnResponse[];
}

export interface CreateRelationshipRequest {
  srcTableId: ULID;
  tgtTableId: ULID;
  name: string;
  kind: string;
  cardinality: string;
  onDelete?: string;
  onUpdate?: string;
  columns?: CreateRelationshipColumnRequest[];
}

export interface CreateRelationshipColumnRequest {
  srcColumnId: ULID;
  tgtColumnId: ULID;
  seqNo: number;
}

export interface UpdateRelationshipNameRequest {
  relationshipId: ULID;
  name: string;
}

export interface UpdateRelationshipCardinalityRequest {
  relationshipId: ULID;
  cardinality: string;
}

export interface AddColumnToRelationshipRequest {
  relationshipId: ULID;
  srcColumnId: ULID;
  tgtColumnId: ULID;
  seqNo: number;
}

export interface RemoveColumnFromRelationshipRequest {
  relationshipId: ULID;
  relationshipColumnId: ULID;
}

export interface DeleteRelationshipRequest {
  relationshipId: ULID;
}
