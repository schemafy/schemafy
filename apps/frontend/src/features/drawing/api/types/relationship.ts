import type { ULID, DatabaseContext } from './common';

export interface RelationshipColumnResponse {
  id: ULID;
  relationshipId: ULID;
  fkColumnId: ULID;
  refColumnId: ULID;
  seqNo: number;
  isAffected: boolean;
}

export interface RelationshipResponse {
  id: ULID;
  srcTableId: ULID;
  tgtTableId: ULID;
  name: string;
  kind: string;
  cardinality: string;
  onDelete: string;
  onUpdate: string;
  fkEnforced: false;
  columns: RelationshipColumnResponse[];
  isAffected: boolean;
  extra?: unknown;
}

export interface CreateRelationshipRequest {
  database: DatabaseContext;
  schemaId: ULID;
  relationship: {
    id: ULID;
    srcTableId: ULID;
    tgtTableId: ULID;
    name: string;
    kind: string;
    cardinality: string;
    onDelete: string;
    onUpdate: string;
    columns: {
      id: ULID;
      relationshipId: ULID;
      fkColumnId: ULID;
      refColumnId: ULID;
      seqNo: number;
    }[];
  };
}

export interface CreateRelationshipColumnRequest {
  fkColumnId: ULID;
  refColumnId: ULID;
  seqNo: number;
}

export interface UpdateRelationshipNameRequest {
  database: DatabaseContext;
  schemaId: ULID;
  relationshipId: ULID;
  newName: string;
}

export interface UpdateRelationshipCardinalityRequest {
  database: DatabaseContext;
  schemaId: ULID;
  relationshipId: ULID;
  cardinality: string;
}

export interface AddColumnToRelationshipRequest {
  database: DatabaseContext;
  schemaId: ULID;
  relationshipId: ULID;
  relationshipColumn: {
    id: ULID;
    relationshipId: ULID;
    fkColumnId: ULID;
    refColumnId: ULID;
    seqNo: number;
  };
}

export interface RemoveColumnFromRelationshipRequest {
  database: DatabaseContext;
  schemaId: ULID;
  relationshipId: ULID;
  relationshipColumnId: ULID;
}

export interface UpdateRelationshipExtraRequest {
  extra: string;
}

export interface DeleteRelationshipRequest {
  database: DatabaseContext;
  schemaId: ULID;
  relationshipId: ULID;
}
