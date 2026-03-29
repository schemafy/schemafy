import { apiClient } from '@/lib/api';
import { createErdMutationConfig } from './mutation-request';
import type {
  MutationResponse,
  RelationshipResponse,
  CreateRelationshipRequest,
  ChangeRelationshipNameRequest,
  ChangeRelationshipKindRequest,
  ChangeRelationshipCardinalityRequest,
  ChangeRelationshipExtraRequest,
  RelationshipColumnResponse,
  AddRelationshipColumnRequest,
  AddRelationshipColumnResponse,
  ChangeRelationshipColumnPositionRequest,
} from './types';

export const createRelationship = async (
  data: CreateRelationshipRequest,
  schemaId: string,
): Promise<MutationResponse<RelationshipResponse>> => {
  const { data: res } = await apiClient.post<
    MutationResponse<RelationshipResponse>
  >('/relationships', data, createErdMutationConfig(schemaId));
  return res;
};

export const getRelationship = async (
  relationshipId: string,
): Promise<RelationshipResponse> => {
  const { data } = await apiClient.get<RelationshipResponse>(
    `/relationships/${relationshipId}`,
  );
  return data;
};

export const getRelationshipsByTableId = async (
  tableId: string,
): Promise<RelationshipResponse[]> => {
  const { data } = await apiClient.get<RelationshipResponse[]>(
    `/tables/${tableId}/relationships`,
  );
  return data;
};

export const changeRelationshipName = async (
  relationshipId: string,
  data: ChangeRelationshipNameRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/relationships/${relationshipId}/name`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const changeRelationshipKind = async (
  relationshipId: string,
  data: ChangeRelationshipKindRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/relationships/${relationshipId}/kind`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const changeRelationshipCardinality = async (
  relationshipId: string,
  data: ChangeRelationshipCardinalityRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/relationships/${relationshipId}/cardinality`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const changeRelationshipExtra = async (
  relationshipId: string,
  data: ChangeRelationshipExtraRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/relationships/${relationshipId}/extra`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const deleteRelationship = async (
  relationshipId: string,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/relationships/${relationshipId}`,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const getRelationshipColumns = async (
  relationshipId: string,
): Promise<RelationshipColumnResponse[]> => {
  const { data } = await apiClient.get<RelationshipColumnResponse[]>(
    `/relationships/${relationshipId}/columns`,
  );
  return data;
};

export const addRelationshipColumn = async (
  relationshipId: string,
  data: AddRelationshipColumnRequest,
  schemaId: string,
): Promise<MutationResponse<AddRelationshipColumnResponse>> => {
  const { data: res } = await apiClient.post<
    MutationResponse<AddRelationshipColumnResponse>
  >(
    `/relationships/${relationshipId}/columns`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const removeRelationshipColumn = async (
  relationshipColumnId: string,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/relationship-columns/${relationshipColumnId}`,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const getRelationshipColumn = async (
  relationshipColumnId: string,
): Promise<RelationshipColumnResponse> => {
  const { data } = await apiClient.get<RelationshipColumnResponse>(
    `/relationship-columns/${relationshipColumnId}`,
  );
  return data;
};

export const changeRelationshipColumnPosition = async (
  relationshipColumnId: string,
  data: ChangeRelationshipColumnPositionRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/relationship-columns/${relationshipColumnId}/position`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};
