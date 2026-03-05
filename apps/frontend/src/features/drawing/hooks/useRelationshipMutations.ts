import { useMutation, useQueryClient } from '@tanstack/react-query';
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
  TableSnapshotResponse,
  CreateRelationshipRequest,
  ChangeRelationshipNameRequest,
  ChangeRelationshipKindRequest,
  ChangeRelationshipCardinalityRequest,
  ChangeRelationshipExtraRequest,
  AddRelationshipColumnRequest,
  ChangeRelationshipColumnPositionRequest,
} from '../api';
import { useErdCache } from './useErdCache';
import { erdKeys } from './query-keys';
import { useErdHistory } from '../history';
import {
  CreateRelationshipCommand,
  DeleteRelationshipCommand,
  ReconnectRelationshipCommand,
  ChangeRelationshipNameCommand,
  ChangeRelationshipKindCommand,
  ChangeRelationshipCardinalityCommand,
  ChangeRelationshipExtraCommand,
} from '../history';

type ChangeRelationshipExtraVars = {
  relationshipId: string;
  data: ChangeRelationshipExtraRequest;
  skipHistory?: boolean;
};

const findRelationshipSnapshot = (
  snapshots: Record<string, TableSnapshotResponse> | undefined,
  relationshipId: string,
) => {
  for (const snapshot of Object.values(snapshots ?? {})) {
    const rel = snapshot.relationships.find((r) => r.relationship.id === relationshipId);
    if (rel) return rel;
  }
  return undefined;
};

export const useCreateRelationshipWithExtra = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { request: CreateRelationshipRequest; extra: string }) =>
      createRelationship({ ...data.request, extra: data.extra }),
    onSuccess: (result, variables) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new CreateRelationshipCommand({
          schemaId,
          queryClient,
          relationshipId: result.data.id,
          createRequest: variables.request,
          extra: variables.extra,
        }),
      );
    },
  });
};

export const useChangeRelationshipName = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: ChangeRelationshipNameRequest;
    }) => changeRelationshipName(relationshipId, data),
    onMutate: ({ relationshipId }) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      const rel = findRelationshipSnapshot(snapshots, relationshipId);
      return { previousName: rel?.relationship.name ?? '' };
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new ChangeRelationshipNameCommand({
          schemaId,
          queryClient,
          relationshipId: variables.relationshipId,
          previousName: context?.previousName ?? '',
          newName: variables.data.newName,
        }),
      );
    },
  });
};

export const useChangeRelationshipKind = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: ChangeRelationshipKindRequest;
    }) => changeRelationshipKind(relationshipId, data),
    onMutate: ({ relationshipId }) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      const rel = findRelationshipSnapshot(snapshots, relationshipId);
      return { previousKind: rel?.relationship.kind ?? '' };
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new ChangeRelationshipKindCommand({
          schemaId,
          queryClient,
          relationshipId: variables.relationshipId,
          previousKind: context?.previousKind ?? '',
          newKind: variables.data.kind,
        }),
      );
    },
  });
};

export const useChangeRelationshipCardinality = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      relationshipId,
      data,
    }: {
      relationshipId: string;
      data: ChangeRelationshipCardinalityRequest;
    }) => changeRelationshipCardinality(relationshipId, data),
    onMutate: ({ relationshipId }) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      const rel = findRelationshipSnapshot(snapshots, relationshipId);
      return { previousCardinality: rel?.relationship.cardinality ?? '' };
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new ChangeRelationshipCardinalityCommand({
          schemaId,
          queryClient,
          relationshipId: variables.relationshipId,
          previousCardinality: context?.previousCardinality ?? '',
          newCardinality: variables.data.cardinality,
        }),
      );
    },
  });
};

export const useChangeRelationshipExtra = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ relationshipId, data }: ChangeRelationshipExtraVars) =>
      changeRelationshipExtra(relationshipId, data),
    onMutate: ({ relationshipId }) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      const rel = findRelationshipSnapshot(snapshots, relationshipId);
      return { previousExtra: rel?.relationship.extra ?? '{}' };
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      if (!variables.skipHistory) {
        history.push(
          new ChangeRelationshipExtraCommand({
            schemaId,
            queryClient,
            relationshipId: variables.relationshipId,
            previousExtra: context?.previousExtra ?? '{}',
            newExtra: variables.data.extra,
          }),
        );
      }
    },
  });
};

export const useDeleteRelationship = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (relationshipId: string) =>
      deleteRelationship(relationshipId),
    onMutate: (relationshipId) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      return { snapshot: findRelationshipSnapshot(snapshots, relationshipId) };
    },
    onSuccess: (result, _variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      if (context?.snapshot) {
        history.push(
          new DeleteRelationshipCommand({
            schemaId,
            queryClient,
            snapshot: context.snapshot,
          }),
        );
      }
    },
  });
};

type ReconnectRelationshipVars = {
  oldRelationshipId: string;
  createRequest: CreateRelationshipRequest;
  extra: string;
};

export const useReconnectRelationship = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ oldRelationshipId, createRequest, extra }: ReconnectRelationshipVars) => {
      const deleteResult = await deleteRelationship(oldRelationshipId);
      const createResult = await createRelationship({ ...createRequest, extra });
      return { deleteResult, createResult };
    },
    onMutate: ({ oldRelationshipId }) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      return { oldSnapshot: findRelationshipSnapshot(snapshots, oldRelationshipId) };
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables([
        ...result.deleteResult.affectedTableIds,
        ...result.createResult.affectedTableIds,
      ]);
      if (context?.oldSnapshot) {
        history.push(
          new ReconnectRelationshipCommand({
            schemaId,
            queryClient,
            oldSnapshot: context.oldSnapshot,
            newRelationshipId: result.createResult.data.id,
            newCreateRequest: variables.createRequest,
            newExtra: variables.extra,
          }),
        );
      }
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
