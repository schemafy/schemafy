import { useCallback, useEffect, useRef, useState } from 'react';
import type { Connection, Edge, EdgeChange } from '@xyflow/react';
import { toast } from 'sonner';
import { useSelectedSchema } from '../contexts';
import { useSchemaSnapshots } from './useSchemaSnapshots';
import {
  useCreateRelationshipWithExtra,
  useChangeRelationshipName,
  useChangeRelationshipKind,
  useChangeRelationshipCardinality,
  useChangeRelationshipExtra,
  useDeleteRelationship,
} from './useRelationshipMutations';
import {
  convertSnapshotsToEdges,
  validateConnection,
  findRelationshipById,
  parseRelationshipExtra,
} from '../utils/relationshipHelpers';
import type { RelationshipConfig, RelationshipExtra, Point } from '../types';
import { RELATIONSHIP_TYPES } from '../types';
import { useLatest } from './useLatest';

const attachControlPointHandler = (
  edge: Edge,
  onControlPointDragEnd: (
    relationshipId: string,
    controlPoint1: Point,
    controlPoint2?: Point,
  ) => void,
) =>
  ({
    ...edge,
    data: {
      ...edge.data,
      onControlPointDragEnd,
    },
  }) as Edge;

export const useRelationships = (relationshipConfig: RelationshipConfig) => {
  const { selectedSchemaId } = useSelectedSchema();
  const { data: snapshotsData } = useSchemaSnapshots(selectedSchemaId);
  const snapshotsRef = useLatest(snapshotsData);
  const relationshipConfigRef = useLatest(relationshipConfig);

  const { mutate: createRelationshipWithExtra } =
    useCreateRelationshipWithExtra(selectedSchemaId);
  const { mutate: changeRelationshipName } =
    useChangeRelationshipName(selectedSchemaId);
  const { mutate: changeRelationshipExtra } =
    useChangeRelationshipExtra(selectedSchemaId);
  const { mutate: changeRelationshipKind } =
    useChangeRelationshipKind(selectedSchemaId);
  const { mutate: changeRelationshipCardinality } =
    useChangeRelationshipCardinality(selectedSchemaId);
  const { mutate: deleteRelationshipMutation } =
    useDeleteRelationship(selectedSchemaId);

  const [selectedRelationship, setSelectedRelationship] = useState<
    string | null
  >(null);
  const relationshipReconnectSuccessful = useRef(true);
  const justFinishedControlPointDrag = useRef(false);

  const updateExtra = useCallback(
    (
      relationshipId: string,
      updater: (extra: RelationshipExtra) => RelationshipExtra,
    ) => {
      const snapshot = findRelationshipById(
        snapshotsRef.current,
        relationshipId,
      );
      if (!snapshot) return;

      const currentExtra = parseRelationshipExtra(snapshot.relationship.extra);
      changeRelationshipExtra({
        relationshipId,
        data: { extra: updater(currentExtra) },
      });
    },
    [changeRelationshipExtra, snapshotsRef],
  );

  const updateRelationshipControlPoint = useCallback(
    (relationshipId: string, controlPoint1: Point, controlPoint2?: Point) => {
      justFinishedControlPointDrag.current = true;
      requestAnimationFrame(() => {
        justFinishedControlPointDrag.current = false;
      });

      updateExtra(relationshipId, (extra: RelationshipExtra) => ({
        ...extra,
        controlPoint1,
        ...(controlPoint2 && { controlPoint2 }),
      }));
    },
    [updateExtra],
  );

  const [relationships, setRelationships] = useState<Edge[]>(() =>
    convertSnapshotsToEdges(snapshotsData).map((edge) =>
      attachControlPointHandler(edge, updateRelationshipControlPoint),
    ),
  );

  useEffect(() => {
    setRelationships((previousRelationships) => {
      const previousEdgesById = new Map(
        previousRelationships.map((relationship) => [
          relationship.id,
          relationship,
        ]),
      );

      const nextRelationships = convertSnapshotsToEdges(snapshotsData).map(
        (edge) => {
          const nextEdge = attachControlPointHandler(
            edge,
            updateRelationshipControlPoint,
          );
          const previousEdge = previousEdgesById.get(edge.id);

          if (previousEdge && hasSameEdgeContent(previousEdge, nextEdge)) {
            return previousEdge;
          }

          return nextEdge;
        },
      );

      return isSameEdgeList(previousRelationships, nextRelationships)
        ? previousRelationships
        : nextRelationships;
    });
  }, [snapshotsData, updateRelationshipControlPoint]);

  const createRelationshipFromValidation = useCallback(
    (
      validation: {
        fkTableId: string;
        pkTableId: string;
      },
      connection: Connection,
    ) => {
      const currentRelationshipConfig = relationshipConfigRef.current;
      const typeConfig = RELATIONSHIP_TYPES[currentRelationshipConfig.type];
      const kind = currentRelationshipConfig.isNonIdentifying
        ? 'NON_IDENTIFYING'
        : 'IDENTIFYING';

      const isSameDirection = connection.source === validation.fkTableId;
      const extra: RelationshipExtra = {
        fkHandle:
          (isSameDirection
            ? connection.sourceHandle
            : connection.targetHandle) ?? undefined,
        pkHandle:
          (isSameDirection
            ? connection.targetHandle
            : connection.sourceHandle) ?? undefined,
      };

      createRelationshipWithExtra({
        request: {
          fkTableId: validation.fkTableId,
          pkTableId: validation.pkTableId,
          kind,
          cardinality: typeConfig.cardinality,
        },
        extra,
      });
    },
    [createRelationshipWithExtra, relationshipConfigRef],
  );

  const onConnect = useCallback(
    (params: Connection) => {
      const validation = validateConnection({
        snapshots: snapshotsRef.current,
        connection: params,
      });

      if (!validation.isValid) {
        if (validation.error) {
          toast.error(validation.error);
        }
        return;
      }

      createRelationshipFromValidation(validation, params);
    },
    [createRelationshipFromValidation, snapshotsRef],
  );

  const onRelationshipsChange = useCallback(
    (changes: EdgeChange[]) => {
      changes
        .filter((change) => change.type === 'remove')
        .forEach((change) => {
          deleteRelationshipMutation(change.id);
        });
    },
    [deleteRelationshipMutation],
  );

  const onRelationshipClick = useCallback(
    (event: React.MouseEvent, relationship: Edge) => {
      if (justFinishedControlPointDrag.current) return;
      event.stopPropagation();
      setSelectedRelationship(relationship.id);
    },
    [],
  );

  const onReconnectStart = useCallback(() => {
    relationshipReconnectSuccessful.current = false;
  }, []);

  const onReconnect = useCallback(
    (oldRelationship: Edge, newConnection: Connection) => {
      const validation = validateConnection({
        snapshots: snapshotsRef.current,
        connection: newConnection,
      });

      if (!validation.isValid) {
        if (validation.error) {
          toast.error(validation.error);
        }
        return;
      }

      relationshipReconnectSuccessful.current = true;

      const isSameTablePair =
        oldRelationship.source === validation.fkTableId &&
        oldRelationship.target === validation.pkTableId;

      if (isSameTablePair) {
        const isSameDirection = newConnection.source === validation.fkTableId;

        updateExtra(oldRelationship.id, (extra) => ({
          ...extra,
          fkHandle:
            (isSameDirection
              ? newConnection.sourceHandle
              : newConnection.targetHandle) ?? undefined,
          pkHandle:
            (isSameDirection
              ? newConnection.targetHandle
              : newConnection.sourceHandle) ?? undefined,
        }));
      } else {
        deleteRelationshipMutation(oldRelationship.id, {
          onSuccess: () => {
            createRelationshipFromValidation(validation, newConnection);
          },
        });
      }
    },
    [
      createRelationshipFromValidation,
      deleteRelationshipMutation,
      snapshotsRef,
      updateExtra,
    ],
  );

  const onReconnectEnd = useCallback(
    (_: MouseEvent | TouchEvent, relationship: Edge) => {
      if (!relationshipReconnectSuccessful.current) {
        deleteRelationshipMutation(relationship.id);
      }
      relationshipReconnectSuccessful.current = true;
    },
    [deleteRelationshipMutation],
  );

  const updateRelationshipConfig = useCallback(
    (relationshipId: string, config: RelationshipConfig) => {
      const relationshipSnapshot = findRelationshipById(
        snapshotsRef.current,
        relationshipId,
      );

      if (!relationshipSnapshot) {
        toast.error('Relationship not found');
        return;
      }

      const { relationship } = relationshipSnapshot;
      const typeConfig = RELATIONSHIP_TYPES[config.type];
      const newKind = config.isNonIdentifying
        ? 'NON_IDENTIFYING'
        : 'IDENTIFYING';

      const needsKindUpdate = relationship.kind !== newKind;
      const needsCardinalityUpdate =
        relationship.cardinality !== typeConfig.cardinality;

      if (needsKindUpdate) {
        changeRelationshipKind(
          { relationshipId, data: { kind: newKind } },
          {
            onSuccess: () => {
              if (needsCardinalityUpdate) {
                changeRelationshipCardinality({
                  relationshipId,
                  data: { cardinality: typeConfig.cardinality },
                });
              }
            },
          },
        );
      } else if (needsCardinalityUpdate) {
        changeRelationshipCardinality({
          relationshipId,
          data: { cardinality: typeConfig.cardinality },
        });
      }
    },
    [changeRelationshipCardinality, changeRelationshipKind, snapshotsRef],
  );

  const deleteRelationship = useCallback(
    (relationshipId: string) => {
      deleteRelationshipMutation(relationshipId);
      setSelectedRelationship(null);
    },
    [deleteRelationshipMutation],
  );

  const updateRelationshipName = useCallback(
    (relationshipId: string, newName: string) => {
      changeRelationshipName({
        relationshipId,
        data: { newName },
      });
    },
    [changeRelationshipName],
  );

  return {
    relationships,
    selectedRelationship,
    onConnect,
    onRelationshipsChange,
    onRelationshipClick,
    onReconnectStart,
    onReconnect,
    onReconnectEnd,
    updateRelationshipConfig,
    deleteRelationship,
    updateRelationshipName,
    setSelectedRelationship,
  };
};

const isSameEdgeList = (prev: Edge[], next: Edge[]) =>
  prev.length === next.length &&
  prev.every((edge, index) => edge === next[index]);

const hasSameEdgeContent = (previousEdge: Edge, nextEdge: Edge) =>
  previousEdge.source === nextEdge.source &&
  previousEdge.target === nextEdge.target &&
  previousEdge.sourceHandle === nextEdge.sourceHandle &&
  previousEdge.targetHandle === nextEdge.targetHandle &&
  previousEdge.label === nextEdge.label &&
  previousEdge.type === nextEdge.type &&
  JSON.stringify(previousEdge.style ?? null) ===
    JSON.stringify(nextEdge.style ?? null) &&
  JSON.stringify(previousEdge.markerStart ?? null) ===
    JSON.stringify(nextEdge.markerStart ?? null) &&
  JSON.stringify(previousEdge.markerEnd ?? null) ===
    JSON.stringify(nextEdge.markerEnd ?? null) &&
  JSON.stringify(previousEdge.labelStyle ?? null) ===
    JSON.stringify(nextEdge.labelStyle ?? null) &&
  JSON.stringify(stripEdgeCallback(previousEdge.data)) ===
    JSON.stringify(stripEdgeCallback(nextEdge.data));

const stripEdgeCallback = (
  data: Edge['data'] | undefined,
): Record<string, unknown> | null => {
  if (!data) return null;

  const { onControlPointDragEnd: _ignored, ...rest } = data as Record<
    string,
    unknown
  >;

  return rest;
};
