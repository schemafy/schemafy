import type { AffectedMappingResponse as ApiAffectedMappingResponse } from '../api/types/common';

export type SourceType = 'RELATIONSHIP' | 'CONSTRAINT';

// API 타입을 그대로 재export
export type {
  PropagatedColumn,
  PropagatedRelationshipColumn,
  PropagatedConstraint,
  PropagatedConstraintColumn,
} from '../api/types/common';

// 그룹화를 위한 확장 타입 (내부 사용)
export type PropagatedEntitiesGroup = {
  sourceType: SourceType;
  columns: ApiAffectedMappingResponse['propagated']['columns'];
  relationshipColumns: ApiAffectedMappingResponse['propagated']['relationshipColumns'];
  constraints: ApiAffectedMappingResponse['propagated']['constraints'];
  constraintColumns: ApiAffectedMappingResponse['propagated']['constraintColumns'];
};

// API response를 그대로 사용
export type AffectedMappingResponse = ApiAffectedMappingResponse;

export interface ServerResponse {
  success: boolean;
  result?: AffectedMappingResponse | null;
}

export type SyncContext = {
  schemaId: string;
  tableId?: string;
  relationshipId?: string;
  constraintId?: string;
  indexId?: string;
};
