import { apiClient } from '@/lib/api';
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
): Promise<MutationResponse<RelationshipResponse>> => {
  const { data: res } = await apiClient.post<
    MutationResponse<RelationshipResponse>
  >('/relationships', data);
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
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/relationships/${relationshipId}/name`,
    data,
  );
  return res;
};

export const changeRelationshipKind = async (
  relationshipId: string,
  data: ChangeRelationshipKindRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/relationships/${relationshipId}/kind`,
    data,
  );
  return res;
};

export const changeRelationshipCardinality = async (
  relationshipId: string,
  data: ChangeRelationshipCardinalityRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/relationships/${relationshipId}/cardinality`,
    data,
  );
  return res;
};

export const changeRelationshipExtra = async (
  relationshipId: string,
  data: ChangeRelationshipExtraRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/relationships/${relationshipId}/extra`,
    data,
  );
  return res;
};

export const deleteRelationship = async (
  relationshipId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/relationships/${relationshipId}`,
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
): Promise<MutationResponse<AddRelationshipColumnResponse>> => {
  const { data: res } = await apiClient.post<
    MutationResponse<AddRelationshipColumnResponse>
  >(`/relationships/${relationshipId}/columns`, data);
  return res;
};

export const removeRelationshipColumn = async (
  relationshipColumnId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/relationship-columns/${relationshipColumnId}`,
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
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/relationship-columns/${relationshipColumnId}/position`,
    data,
  );
  return res;
};
