import { useState, useRef, useMemo } from 'react';
import type { Connection, Edge, EdgeChange } from '@xyflow/react';
import { toast } from 'sonner';
import { useSelectedSchema } from '../contexts';
import { useSchemaSnapshots } from './useSchemaSnapshots';
import {
  useCreateRelationship,
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

  const createRelationshipMutation = useCreateRelationship(selectedSchemaId);
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

  const updateExtra = (
    relationshipId: string,
    updater: (extra: RelationshipExtra) => RelationshipExtra,
  ) => {
    if (!snapshotsData) return;
    const snapshot = findRelationshipById(snapshotsData, relationshipId);
    if (!snapshot) return;

    const currentExtra = parseRelationshipExtra(
      snapshot.relationship.extra,
    );
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
    updateExtra(relationshipId, (extra: RelationshipExtra) => ({
      ...extra,
      controlPoint1,
      ...(controlPoint2 && { controlPoint2 }),
    }));
  };

  const controlPointCallbackRef = useRef(updateRelationshipControlPoint);
  controlPointCallbackRef.current = updateRelationshipControlPoint;

  const relationships = useMemo(() => {
    if (!snapshotsData) {
      return [];
    }

    const edges = convertSnapshotsToEdges(snapshotsData);
    return edges.map((edge) => ({
      ...edge,
      data: {
        ...edge.data,
        onControlPointDragEnd: (...args: Parameters<typeof updateRelationshipControlPoint>) =>
          controlPointCallbackRef.current(...args),
      },
    }));
  }, [snapshotsData]);

  const createRelationshipFromValidation = (validation: {
    fkTableId: string;
    pkTableId: string;
  }) => {
    const typeConfig = RELATIONSHIP_TYPES[relationshipConfig.type];
    const kind = relationshipConfig.isNonIdentifying
      ? 'NON_IDENTIFYING'
      : 'IDENTIFYING';

    createRelationshipMutation.mutate({
      fkTableId: validation.fkTableId,
      pkTableId: validation.pkTableId,
      kind,
      cardinality: typeConfig.cardinality,
    });
  };

  const onConnect = (params: Connection) => {
    if (!snapshotsData) return;

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

    createRelationshipFromValidation(validation);
  };

  const onRelationshipsChange = (changes: EdgeChange[]) => {
    changes
      .filter((change) => change.type === 'remove')
      .forEach((change) => {
        deleteRelationshipMutation.mutate(change.id);
      });
  };

  const onRelationshipClick = (event: React.MouseEvent, relationship: Edge) => {
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
    if (!snapshotsData) return;

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
      const isSameDirection =
        newConnection.source === validation.fkTableId;

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
      createRelationshipFromValidation(validation);
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
    if (!snapshotsData) {
      toast.error('No snapshots data');
      return;
    }

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

  const changeRelationshipName = (relationshipId: string, newName: string) => {
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
    changeRelationshipName,
    setSelectedRelationship,
  };
};
