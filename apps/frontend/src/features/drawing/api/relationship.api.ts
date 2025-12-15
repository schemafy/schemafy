import { api } from '@/lib/api/helpers';
import type {
  RelationshipResponse,
  CreateRelationshipRequest,
  UpdateRelationshipNameRequest,
  UpdateRelationshipCardinalityRequest,
  UpdateRelationshipExtraRequest,
  AddColumnToRelationshipRequest,
  RemoveColumnFromRelationshipRequest,
  DeleteRelationshipRequest,
} from './types/relationship';
import type { AffectedMappingResponse } from './types/common';

export const createRelationshipAPI = (
  data: CreateRelationshipRequest,
  extra?: string,
) =>
  api.post<AffectedMappingResponse>('/relationships', data, {
    params: extra ? { extra } : undefined,
  });

export const getRelationshipAPI = (relationshipId: string) =>
  api.get<RelationshipResponse>(`/relationships/${relationshipId}`);

export const updateRelationshipNameAPI = (
  relationshipId: string,
  data: UpdateRelationshipNameRequest,
) =>
  api.put<RelationshipResponse>(`/relationships/${relationshipId}/name`, data);

export const updateRelationshipCardinalityAPI = (
  relationshipId: string,
  data: UpdateRelationshipCardinalityRequest,
) =>
  api.put<RelationshipResponse>(
    `/relationships/${relationshipId}/cardinality`,
    data,
  );

export const updateRelationshipExtraAPI = (
  relationshipId: string,
  data: UpdateRelationshipExtraRequest,
) =>
  api.put<RelationshipResponse>(
    `/relationships/${relationshipId}/extra`,
    data,
  );

export const addColumnToRelationshipAPI = (
  relationshipId: string,
  data: AddColumnToRelationshipRequest,
) =>
  api.post<AffectedMappingResponse>(
    `/relationships/${relationshipId}/columns`,
    data,
  );

export const removeColumnFromRelationshipAPI = (
  relationshipId: string,
  columnId: string,
  data: RemoveColumnFromRelationshipRequest,
) =>
  api.delete<null>(`/relationships/${relationshipId}/columns/${columnId}`, {
    data,
  });

export const deleteRelationshipAPI = (
  relationshipId: string,
  data: DeleteRelationshipRequest,
) => api.delete<null>(`/relationships/${relationshipId}`, { data });
