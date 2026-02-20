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
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: async (data: {
      request: CreateRelationshipRequest;
      extra: string;
    }) => {
      const createResult = await createRelationship(data.request);
      if (createResult.data) {
        const extraResult = await changeRelationshipExtra(
          createResult.data.id,
          { extra: data.extra },
        );
        return extraResult;
      }
      return createResult;
    },
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeRelationshipName = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: ChangeRelationshipNameRequest;
    }) => changeRelationshipName(relationshipId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeRelationshipKind = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: ChangeRelationshipKindRequest;
    }) => changeRelationshipKind(relationshipId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeRelationshipCardinality = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: ChangeRelationshipCardinalityRequest;
    }) => changeRelationshipCardinality(relationshipId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeRelationshipExtra = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: ChangeRelationshipExtraRequest;
    }) => changeRelationshipExtra(relationshipId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useDeleteRelationship = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (relationshipId: string) => deleteRelationship(relationshipId),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useAddRelationshipColumn = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: AddRelationshipColumnRequest;
    }) => addRelationshipColumn(relationshipId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useRemoveRelationshipColumn = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (relationshipColumnId: string) =>
      removeRelationshipColumn(relationshipColumnId),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeRelationshipColumnPosition = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      relationshipColumnId,
      data,
    }: {
      relationshipColumnId: string;
      data: ChangeRelationshipColumnPositionRequest;
    }) => changeRelationshipColumnPosition(relationshipColumnId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};
