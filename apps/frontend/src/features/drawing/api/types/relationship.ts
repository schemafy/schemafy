import type { ULID, DatabaseContext } from './common';

export type RelationshipColumnResponse = {
  id: ULID;
  relationshipId: ULID;
  fkColumnId: ULID;
  pkColumnId: ULID;
  seqNo: number;
  isAffected: boolean;
};

export type RelationshipResponse = {
  id: ULID;
  fkTableId: ULID;
  pkTableId: ULID;
  name: string;
  kind: string;
  cardinality: string;
  onDelete: string;
  onUpdate: string;
  fkEnforced: false;
  columns: RelationshipColumnResponse[];
  isAffected: boolean;
  extra?: unknown;
};

export type CreateRelationshipRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  relationship: {
    id: ULID;
    fkTableId: ULID;
    pkTableId: ULID;
    name: string;
    kind: string;
    cardinality: 'ONE_TO_ONE' | 'ONE_TO_MANY';
    onDelete: string;
    onUpdate:
      | 'NO_ACTION_UPDATE'
      | 'RESTRICT_UPDATE'
      | 'CASCADE_UPDATE'
      | 'SET_NULL_UPDATE'
      | 'SET_DEFAULT_UPDATE';
    columns: {
      id: ULID;
      relationshipId: ULID;
      fkColumnId: ULID;
      pkColumnId: ULID;
      seqNo: number;
    }[];
  };
};

export type CreateRelationshipColumnRequest = {
  fkColumnId: ULID;
  pkColumnId: ULID;
  seqNo: number;
};

export type UpdateRelationshipNameRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  relationshipId: ULID;
  newName: string;
};

export type UpdateRelationshipCardinalityRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  relationshipId: ULID;
  cardinality: 'ONE_TO_ONE' | 'ONE_TO_MANY';
};

export type UpdateRelationshipKindRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  relationshipId: ULID;
  kind: 'IDENTIFYING' | 'NON_IDENTIFYING';
};

export type AddColumnToRelationshipRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  relationshipId: ULID;
  relationshipColumn: {
    id: ULID;
    relationshipId: ULID;
    fkColumnId: ULID;
    pkColumnId: ULID;
    seqNo: number;
  };
};

export type RemoveColumnFromRelationshipRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  relationshipId: ULID;
  relationshipColumnId: ULID;
};

export type UpdateRelationshipExtraRequest = {
  extra: string;
};

export type DeleteRelationshipRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  relationshipId: ULID;
};
