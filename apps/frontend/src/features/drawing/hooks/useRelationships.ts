import { useState, useRef, useMemo, useCallback } from 'react';
import type { Connection, Edge, EdgeChange } from '@xyflow/react';
import { toast } from 'sonner';
import { useSelectedSchema } from '../contexts';
import { useSchemaSnapshots } from './useSchemaSnapshots';
import {
  useCreateRelationship,
  useChangeRelationshipName,
  useChangeRelationshipExtra,
  useDeleteRelationship,
} from './useRelationshipMutations';
import {
  convertSnapshotsToEdges,
  validateConnection,
  shouldRecreateRelationship,
  findRelationshipById,
  parseRelationshipExtra,
} from '../utils/relationshipHelpers';
import type { RelationshipConfig, RelationshipExtra, Point } from '../types';
import { RELATIONSHIP_TYPES } from '../types';

export const useRelationships = (relationshipConfig: RelationshipConfig) => {
  const { selectedSchemaId } = useSelectedSchema();
  const { data: snapshotsData } = useSchemaSnapshots(selectedSchemaId || '');

  const createRelationshipMutation = useCreateRelationship(
    selectedSchemaId || '',
  );
  const changeRelationshipNameMutation = useChangeRelationshipName(
    selectedSchemaId || '',
  );
  const changeRelationshipExtraMutation = useChangeRelationshipExtra(
    selectedSchemaId || '',
  );
  const deleteRelationshipMutation = useDeleteRelationship(
    selectedSchemaId || '',
  );

  const [selectedRelationship, setSelectedRelationship] = useState<
    string | null
  >(null);
  const relationshipReconnectSuccessful = useRef(true);

  const updateRelationshipControlPoint = useCallback(
    (relationshipId: string, controlPoint1: Point, controlPoint2?: Point) => {
      if (!snapshotsData) return;

      const relationshipSnapshot = findRelationshipById(
        snapshotsData,
        relationshipId,
      );
      if (!relationshipSnapshot) return;

      const currentExtra = parseRelationshipExtra(
        relationshipSnapshot.relationship.extra,
      );

      const updatedExtra: RelationshipExtra = {
        ...currentExtra,
        controlPoint1,
        ...(controlPoint2 && { controlPoint2 }),
      };

      changeRelationshipExtraMutation.mutate({
        relationshipId,
        data: { extra: JSON.stringify(updatedExtra) },
      });
    },
    [snapshotsData, changeRelationshipExtraMutation],
  );

  const relationships = useMemo(() => {
    if (!selectedSchemaId || !snapshotsData) {
      return [];
    }

    const edges = convertSnapshotsToEdges(snapshotsData);
    return edges.map((edge) => ({
      ...edge,
      data: {
        ...edge.data,
        onControlPointDragEnd: updateRelationshipControlPoint,
      },
    }));
  }, [selectedSchemaId, snapshotsData, updateRelationshipControlPoint]);

  const createRelationshipFromValidation = (
    validation: ReturnType<typeof validateConnection>,
  ) => {
    const typeConfig = RELATIONSHIP_TYPES[relationshipConfig.type];
    const kind = relationshipConfig.isNonIdentifying
      ? 'NON_IDENTIFYING'
      : 'IDENTIFYING';

    createRelationshipMutation.mutate({
      fkTableId: validation.fkTableId!,
      pkTableId: validation.pkTableId!,
      kind,
      cardinality: typeConfig.cardinality,
    });
  };

  const onConnect = (params: Connection) => {
    if (!selectedSchemaId || !snapshotsData) return;

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
    if (!selectedSchemaId) return;

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

  const onReconnect = (oldRelationship: Edge, newConnection: Connection) => {
    if (!selectedSchemaId || !snapshotsData) return;

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
    deleteRelationshipMutation.mutate(oldRelationship.id);
    createRelationshipFromValidation(validation);
  };

  const onReconnectEnd = (_: MouseEvent | TouchEvent, relationship: Edge) => {
    if (!selectedSchemaId) return;

    if (!relationshipReconnectSuccessful.current) {
      deleteRelationshipMutation.mutate(relationship.id);
    }
    relationshipReconnectSuccessful.current = true;
  };

  const updateRelationshipConfig = (
    relationshipId: string,
    config: RelationshipConfig,
  ) => {
    if (!selectedSchemaId || !snapshotsData) {
      toast.error('No schema selected');
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

    if (
      shouldRecreateRelationship(
        relationship.kind,
        relationship.cardinality,
        newKind,
        typeConfig.cardinality,
      )
    ) {
      deleteRelationshipMutation.mutate(relationshipId);
      createRelationshipMutation.mutate({
        fkTableId: relationship.fkTableId,
        pkTableId: relationship.pkTableId,
        kind: newKind,
        cardinality: typeConfig.cardinality,
      });
    }
  };

  const deleteRelationship = (relationshipId: string) => {
    if (!selectedSchemaId) {
      toast.error('No schema selected');
      return;
    }

    deleteRelationshipMutation.mutate(relationshipId);
    setSelectedRelationship(null);
  };

  const changeRelationshipName = (relationshipId: string, newName: string) => {
    if (!selectedSchemaId) {
      toast.error('No schema selected');
      return;
    }

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
