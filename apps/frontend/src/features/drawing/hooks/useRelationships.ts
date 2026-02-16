import { useState, useRef, useMemo } from 'react';
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

export const useRelationships = (relationshipConfig: RelationshipConfig) => {
  const { selectedSchemaId } = useSelectedSchema();
  const { data: snapshotsData } = useSchemaSnapshots(selectedSchemaId);

  const createRelationshipWithExtraMutation =
    useCreateRelationshipWithExtra(selectedSchemaId);
  const changeRelationshipNameMutation =
    useChangeRelationshipName(selectedSchemaId);
  const changeRelationshipExtraMutation =
    useChangeRelationshipExtra(selectedSchemaId);
  const changeRelationshipKindMutation =
    useChangeRelationshipKind(selectedSchemaId);
  const changeRelationshipCardinalityMutation =
    useChangeRelationshipCardinality(selectedSchemaId);
  const deleteRelationshipMutation = useDeleteRelationship(selectedSchemaId);

  const [selectedRelationship, setSelectedRelationship] = useState<
    string | null
  >(null);
  const relationshipReconnectSuccessful = useRef(true);
  const justFinishedControlPointDrag = useRef(false);

  const updateExtra = (
    relationshipId: string,
    updater: (extra: RelationshipExtra) => RelationshipExtra,
  ) => {
    const snapshot = findRelationshipById(snapshotsData, relationshipId);
    if (!snapshot) return;

    const currentExtra = parseRelationshipExtra(snapshot.relationship.extra);
    changeRelationshipExtraMutation.mutate({
      relationshipId,
      data: { extra: JSON.stringify(updater(currentExtra)) },
    });
  };

  const updateRelationshipControlPoint = (
    relationshipId: string,
    controlPoint1: Point,
    controlPoint2?: Point,
  ) => {
    justFinishedControlPointDrag.current = true;
    requestAnimationFrame(() => {
      justFinishedControlPointDrag.current = false;
    });

    updateExtra(relationshipId, (extra: RelationshipExtra) => ({
      ...extra,
      controlPoint1,
      ...(controlPoint2 && { controlPoint2 }),
    }));
  };

  const controlPointCallbackRef = useRef(updateRelationshipControlPoint);
  controlPointCallbackRef.current = updateRelationshipControlPoint;

  const relationships = useMemo(() => {
    const edges = convertSnapshotsToEdges(snapshotsData);
    return edges.map((edge) => ({
      ...edge,
      data: {
        ...edge.data,
        onControlPointDragEnd: (
          ...args: Parameters<typeof updateRelationshipControlPoint>
        ) => controlPointCallbackRef.current(...args),
      },
    }));
  }, [snapshotsData]);

  const createRelationshipFromValidation = (
    validation: {
      fkTableId: string;
      pkTableId: string;
    },
    connection: Connection,
  ) => {
    const typeConfig = RELATIONSHIP_TYPES[relationshipConfig.type];
    const kind = relationshipConfig.isNonIdentifying
      ? 'NON_IDENTIFYING'
      : 'IDENTIFYING';

    const isSameDirection = connection.source === validation.fkTableId;
    const extra: RelationshipExtra = {
      fkHandle:
        (isSameDirection ? connection.sourceHandle : connection.targetHandle) ??
        undefined,
      pkHandle:
        (isSameDirection ? connection.targetHandle : connection.sourceHandle) ??
        undefined,
    };

    createRelationshipWithExtraMutation.mutate({
      request: {
        fkTableId: validation.fkTableId,
        pkTableId: validation.pkTableId,
        kind,
        cardinality: typeConfig.cardinality,
      },
      extra: JSON.stringify(extra),
    });
  };

  const onConnect = (params: Connection) => {
    const validation = validateConnection({
      snapshots: snapshotsData,
      connection: params,
    });

    if (!validation.isValid) {
      if (validation.error) {
        toast.error(validation.error);
      }
      return;
    }

    createRelationshipFromValidation(validation, params);
  };

  const onRelationshipsChange = (changes: EdgeChange[]) => {
    changes
      .filter((change) => change.type === 'remove')
      .forEach((change) => {
        deleteRelationshipMutation.mutate(change.id);
      });
  };

  const onRelationshipClick = (event: React.MouseEvent, relationship: Edge) => {
    if (justFinishedControlPointDrag.current) return;
    event.stopPropagation();
    setSelectedRelationship(relationship.id);
  };

  const onReconnectStart = () => {
    relationshipReconnectSuccessful.current = false;
  };

  const onReconnect = async (
    oldRelationship: Edge,
    newConnection: Connection,
  ) => {
    const validation = validateConnection({
      snapshots: snapshotsData,
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
      await deleteRelationshipMutation.mutateAsync(oldRelationship.id);
      createRelationshipFromValidation(validation, newConnection);
    }
  };

  const onReconnectEnd = (_: MouseEvent | TouchEvent, relationship: Edge) => {
    if (!relationshipReconnectSuccessful.current) {
      deleteRelationshipMutation.mutate(relationship.id);
    }
    relationshipReconnectSuccessful.current = true;
  };

  const updateRelationshipConfig = (
    relationshipId: string,
    config: RelationshipConfig,
  ) => {
    const relationshipSnapshot = findRelationshipById(
      snapshotsData,
      relationshipId,
    );

    if (!relationshipSnapshot) {
      toast.error('Relationship not found');
      return;
    }

    const { relationship } = relationshipSnapshot;
    const typeConfig = RELATIONSHIP_TYPES[config.type];
    const newKind = config.isNonIdentifying ? 'NON_IDENTIFYING' : 'IDENTIFYING';

    if (relationship.kind !== newKind) {
      changeRelationshipKindMutation.mutate({
        relationshipId,
        data: { kind: newKind },
      });
    }

    if (relationship.cardinality !== typeConfig.cardinality) {
      changeRelationshipCardinalityMutation.mutate({
        relationshipId,
        data: { cardinality: typeConfig.cardinality },
      });
    }
  };

  const deleteRelationship = (relationshipId: string) => {
    deleteRelationshipMutation.mutate(relationshipId);
    setSelectedRelationship(null);
  };

  const updateRelationshipName = (relationshipId: string, newName: string) => {
    changeRelationshipNameMutation.mutate({
      relationshipId,
      data: { newName },
    });
  };

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
