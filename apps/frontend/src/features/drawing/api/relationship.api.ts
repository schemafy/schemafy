import { api } from '@/lib/api/helpers';
import type {
  RelationshipResponse,
  CreateRelationshipRequest,
  UpdateRelationshipNameRequest,
  UpdateRelationshipCardinalityRequest,
  AddColumnToRelationshipRequest,
  RemoveColumnFromRelationshipRequest,
  DeleteRelationshipRequest,
} from './types/relationship';
import type { AffectedMappingResponse } from './types/common';

export const createRelationship = (
  data: CreateRelationshipRequest,
  extra?: string,
) =>
  api.post<AffectedMappingResponse>('/api/relationships', data, {
    params: extra ? { extra } : undefined,
  });

export const getRelationship = (relationshipId: string) =>
  api.get<RelationshipResponse>(`/api/relationships/${relationshipId}`);

export const updateRelationshipName = (
  relationshipId: string,
  data: UpdateRelationshipNameRequest,
) =>
  api.put<RelationshipResponse>(
    `/api/relationships/${relationshipId}/name`,
    data,
  );

export const updateRelationshipCardinality = (
  relationshipId: string,
  data: UpdateRelationshipCardinalityRequest,
) =>
  api.put<RelationshipResponse>(
    `/api/relationships/${relationshipId}/cardinality`,
    data,
  );

export const addColumnToRelationship = (
  relationshipId: string,
  data: AddColumnToRelationshipRequest,
) =>
  api.post<AffectedMappingResponse>(
    `/api/relationships/${relationshipId}/columns`,
    data,
  );

export const removeColumnFromRelationship = (
  relationshipId: string,
  columnId: string,
  data: RemoveColumnFromRelationshipRequest,
) =>
  api.delete<null>(`/api/relationships/${relationshipId}/columns/${columnId}`, {
    data,
  });

export const deleteRelationship = (
  relationshipId: string,
  data: DeleteRelationshipRequest,
) => api.delete<null>(`/api/relationships/${relationshipId}`, { data });
