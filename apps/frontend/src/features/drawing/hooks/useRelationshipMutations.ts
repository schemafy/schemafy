import { useMutation } from '@tanstack/react-query';
import {
  createRelationship,
  changeRelationshipName,
  changeRelationshipKind,
  changeRelationshipCardinality,
  changeRelationshipExtra,
  deleteRelationship,
  addRelationshipColumn,
  removeRelationshipColumn,
  changeRelationshipColumnPosition,
} from '../api';
import type {
  CreateRelationshipRequest,
  ChangeRelationshipNameRequest,
  ChangeRelationshipKindRequest,
  ChangeRelationshipCardinalityRequest,
  ChangeRelationshipExtraRequest,
  AddRelationshipColumnRequest,
  ChangeRelationshipColumnPositionRequest,
} from '../api';
import { useErdCache } from './useErdCache';

export const useCreateRelationshipWithExtra = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (data: {
      request: CreateRelationshipRequest;
      extra: NonNullable<CreateRelationshipRequest['extra']>;
    }) => createRelationship({ ...data.request, extra: data.extra }, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeRelationshipName = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: ChangeRelationshipNameRequest;
    }) => changeRelationshipName(relationshipId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeRelationshipKind = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: ChangeRelationshipKindRequest;
    }) => changeRelationshipKind(relationshipId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeRelationshipCardinality = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: ChangeRelationshipCardinalityRequest;
    }) => changeRelationshipCardinality(relationshipId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeRelationshipExtra = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: ChangeRelationshipExtraRequest;
    }) => changeRelationshipExtra(relationshipId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useDeleteRelationship = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (relationshipId: string) =>
      deleteRelationship(relationshipId, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useAddRelationshipColumn = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: AddRelationshipColumnRequest;
    }) => addRelationshipColumn(relationshipId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useRemoveRelationshipColumn = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (relationshipColumnId: string) =>
      removeRelationshipColumn(relationshipColumnId, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeRelationshipColumnPosition = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipColumnId,
      data,
    }: {
      relationshipColumnId: string;
      data: ChangeRelationshipColumnPositionRequest;
    }) =>
      changeRelationshipColumnPosition(relationshipColumnId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};
