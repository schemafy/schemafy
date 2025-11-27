import type { Connection, Edge, EdgeChange } from '@xyflow/react';
import { useState, useRef, useEffect } from 'react';
import { applyEdgeChanges } from '@xyflow/react';
import { autorun } from 'mobx';
import { toast } from 'sonner';
import { ErdStore } from '@/store';
import type { RelationshipConfig } from '../types';
import { RELATIONSHIP_TYPES, type RelationshipExtra } from '../types';
import {
  convertRelationshipsToEdges,
  createRelationshipFromConnection,
  findRelationshipInSchema,
  shouldRecreateRelationship,
} from '../utils/relationshipHelpers';

export const useRelationships = (relationshipConfig: RelationshipConfig) => {
  const erdStore = ErdStore.getInstance();
  const [selectedRelationship, setSelectedRelationship] = useState<
    string | null
  >(null);
  const relationshipReconnectSuccessful = useRef(true);

  const updateRelationshipControlPoint = (
    relationshipId: string,
    controlPoint1X: number,
    controlPoint1Y: number,
    controlPoint2X?: number,
    controlPoint2Y?: number,
  ) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema) return;

    const currentRelationship = findRelationshipInSchema(
      selectedSchema,
      relationshipId,
    );
    if (!currentRelationship) return;

    const currentExtra = (currentRelationship.extra || {}) as RelationshipExtra;

    const updatedExtra: RelationshipExtra = {
      ...currentExtra,
      controlPoint1X,
      controlPoint1Y,
    };

    if (controlPoint2X !== undefined && controlPoint2Y !== undefined) {
      updatedExtra.controlPoint2X = controlPoint2X;
      updatedExtra.controlPoint2Y = controlPoint2Y;
    }

    erdStore.updateRelationshipExtra(
      selectedSchemaId,
      relationshipId,
      updatedExtra,
    );
  };

  const getRelationships = (): Edge[] => {
    const selectedSchema = erdStore.selectedSchema;
    if (!selectedSchema) return [];

    const edges = convertRelationshipsToEdges(selectedSchema);

    return edges.map((edge) => ({
      ...edge,
      data: {
        ...edge.data,
        onControlPointDragEnd: updateRelationshipControlPoint,
      },
    }));
  };

  const [relationships, setRelationships] = useState<Edge[]>(() =>
    getRelationships(),
  );

  useEffect(() => {
    const dispose = autorun(() => {
      const state = erdStore.erdState;
      if (state.state === 'loaded') {
        setRelationships(getRelationships());
      }
    });

    return () => dispose();
  }, []);

  const onConnect = (params: Connection) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (
      !selectedSchemaId ||
      !selectedSchema ||
      !params.source ||
      !params.target
    ) {
      return;
    }

    const newRelationship = createRelationshipFromConnection({
      schema: selectedSchema,
      connection: params,
      relationshipConfig,
    });

    if (newRelationship) {
      erdStore.createRelationship(selectedSchemaId, newRelationship);
    }
  };

  const onRelationshipsChange = (changes: EdgeChange[]) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    if (!selectedSchemaId) return;

    changes
      .filter((change) => change.type === 'remove')
      .forEach((change) => {
        erdStore.deleteRelationship(selectedSchemaId, change.id);
      });

    setRelationships((edges) => applyEdgeChanges(changes, edges) as Edge[]);
  };

  const onRelationshipClick = (event: React.MouseEvent, relationship: Edge) => {
    event.stopPropagation();
    setSelectedRelationship(relationship.id);
  };

  const onReconnectStart = () => {
    relationshipReconnectSuccessful.current = false;
  };

  const onReconnect = (oldRelationship: Edge, newConnection: Connection) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema) return;

    relationshipReconnectSuccessful.current = true;
    erdStore.deleteRelationship(selectedSchemaId, oldRelationship.id);

    const newRelationship = createRelationshipFromConnection({
      schema: selectedSchema,
      connection: newConnection,
      relationshipConfig,
    });

    if (newRelationship) {
      erdStore.createRelationship(selectedSchemaId, newRelationship);
    }
  };

  const onReconnectEnd = (_: MouseEvent | TouchEvent, relationship: Edge) => {
    const selectedSchemaId = erdStore.selectedSchemaId;

    if (!selectedSchemaId) return;

    if (!relationshipReconnectSuccessful.current) {
      erdStore.deleteRelationship(selectedSchemaId, relationship.id);
    }
    relationshipReconnectSuccessful.current = true;
  };

  const updateRelationshipConfig = (
    relationshipId: string,
    config: RelationshipConfig,
  ) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema) {
      toast.error('No schema selected');
      return;
    }

    const currentRelationship = findRelationshipInSchema(
      selectedSchema,
      relationshipId,
    );
    if (!currentRelationship) {
      toast.error('Relationship not found');
      return;
    }

    const typeConfig = RELATIONSHIP_TYPES[config.type];
    const newKind = config.isNonIdentifying ? 'NON_IDENTIFYING' : 'IDENTIFYING';
    const currentExtra = (currentRelationship.extra || {}) as RelationshipExtra;

    if (
      shouldRecreateRelationship(
        currentRelationship,
        newKind,
        typeConfig.cardinality,
      )
    ) {
      erdStore.deleteRelationship(selectedSchemaId, relationshipId);
      erdStore.createRelationship(selectedSchemaId, {
        ...currentRelationship,
        kind: newKind,
        cardinality: typeConfig.cardinality,
        extra: currentExtra,
      });
    }
  };

  const deleteRelationship = (relationshipId: string) => {
    const selectedSchemaId = erdStore.selectedSchemaId;

    if (!selectedSchemaId) {
      toast.error('No schema selected');
      return;
    }

    erdStore.deleteRelationship(selectedSchemaId, relationshipId);
    setSelectedRelationship(null);
  };

  const changeRelationshipName = (relationshipId: string, newName: string) => {
    const selectedSchemaId = erdStore.selectedSchemaId;

    if (!selectedSchemaId) {
      toast.error('No schema selected');
      return;
    }

    erdStore.changeRelationshipName(selectedSchemaId, relationshipId, newName);
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
