import { type ApiResponse, bffClient } from '@/lib/api';
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
  const { data: res } = await bffClient.post<
    ApiResponse<MutationResponse<RelationshipResponse>>
  >('/relationships', data);
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const getRelationship = async (
  relationshipId: string,
): Promise<RelationshipResponse> => {
  const { data } = await bffClient.get<ApiResponse<RelationshipResponse>>(
    `/relationships/${relationshipId}`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const getRelationshipsByTableId = async (
  tableId: string,
): Promise<RelationshipResponse[]> => {
  const { data } = await bffClient.get<ApiResponse<RelationshipResponse[]>>(
    `/tables/${tableId}/relationships`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const changeRelationshipName = async (
  relationshipId: string,
  data: ChangeRelationshipNameRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/relationships/${relationshipId}/name`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const changeRelationshipKind = async (
  relationshipId: string,
  data: ChangeRelationshipKindRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/relationships/${relationshipId}/kind`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const changeRelationshipCardinality = async (
  relationshipId: string,
  data: ChangeRelationshipCardinalityRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/relationships/${relationshipId}/cardinality`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const changeRelationshipExtra = async (
  relationshipId: string,
  data: ChangeRelationshipExtraRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/relationships/${relationshipId}/extra`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const deleteRelationship = async (
  relationshipId: string,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.delete<ApiResponse<MutationResponse>>(
    `/relationships/${relationshipId}`,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const getRelationshipColumns = async (
  relationshipId: string,
): Promise<RelationshipColumnResponse[]> => {
  const { data } = await bffClient.get<
    ApiResponse<RelationshipColumnResponse[]>
  >(`/relationships/${relationshipId}/columns`);
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const addRelationshipColumn = async (
  relationshipId: string,
  data: AddRelationshipColumnRequest,
): Promise<MutationResponse<AddRelationshipColumnResponse>> => {
  const { data: res } = await bffClient.post<
    ApiResponse<MutationResponse<AddRelationshipColumnResponse>>
  >(`/relationships/${relationshipId}/columns`, data);
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const removeRelationshipColumn = async (
  relationshipColumnId: string,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.delete<ApiResponse<MutationResponse>>(
    `/relationship-columns/${relationshipColumnId}`,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const getRelationshipColumn = async (
  relationshipColumnId: string,
): Promise<RelationshipColumnResponse> => {
  const { data } = await bffClient.get<ApiResponse<RelationshipColumnResponse>>(
    `/relationship-columns/${relationshipColumnId}`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const changeRelationshipColumnPosition = async (
  relationshipColumnId: string,
  data: ChangeRelationshipColumnPositionRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/relationship-columns/${relationshipColumnId}/position`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};
