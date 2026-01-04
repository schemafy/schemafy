import type { AffectedMappingResponse } from '../api/types/common';

export type SourceType = 'RELATIONSHIP' | 'CONSTRAINT';

export type PropagatedEntitiesGroup = {
  sourceType: SourceType;
  columns: AffectedMappingResponse['propagated']['columns'];
  relationshipColumns: AffectedMappingResponse['propagated']['relationshipColumns'];
  constraints: AffectedMappingResponse['propagated']['constraints'];
  constraintColumns: AffectedMappingResponse['propagated']['constraintColumns'];
};

export type SyncContext = {
  schemaId: string;
  tableId?: string;
  relationshipId?: string;
  constraintId?: string;
  indexId?: string;
};

export type EntityType =
  | 'schema'
  | 'table'
  | 'column'
  | 'index'
  | 'indexColumn'
  | 'constraint'
  | 'constraintColumn'
  | 'relationship'
  | 'relationshipColumn';
