import type { Connection, Edge, EdgeChange } from '@xyflow/react';
import { useState, useRef, useEffect } from 'react';
import { applyEdgeChanges } from '@xyflow/react';
import { reaction } from 'mobx';
import { ErdStore } from '@/store';
import type { RelationshipConfig } from '../types';
import { RELATIONSHIP_TYPES } from '../types';
import {
  convertRelationshipsToEdges,
  createRelationshipFromConnection,
  findRelationshipInSchema,
} from '../utils/relationshipHelpers';

type RelationshipExtra = {
  sourceHandle?: string;
  targetHandle?: string;
  controlPointX?: number;
  controlPointY?: number;
};

export const useRelationships = (relationshipConfig: RelationshipConfig) => {
  const erdStore = ErdStore.getInstance();
  const [selectedRelationship, setSelectedRelationship] = useState<string | null>(null);
  const relationshipReconnectSuccessful = useRef(true);

  const updateRelationshipControlPoint = (relationshipId: string, controlPointX: number, controlPointY: number) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema) return;

    const currentRelationship = findRelationshipInSchema(selectedSchema, relationshipId);
    if (!currentRelationship) return;

    const currentExtra = (currentRelationship.extra || {}) as RelationshipExtra;

    erdStore.updateRelationshipExtra(selectedSchemaId, relationshipId, {
      ...currentExtra,
      controlPointX,
      controlPointY,
    });
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

  const [relationships, setRelationships] = useState<Edge[]>(() => getRelationships());

  useEffect(() => {
    const dispose = reaction(
      () => ({
        schema: erdStore.selectedSchema,
        schemaId: erdStore.selectedSchemaId,
      }),
      () => {
        setRelationships(getRelationships());
      },
    );

    return () => dispose();
  }, []);

  const onConnect = (params: Connection) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema || !params.source || !params.target) {
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

  const updateRelationshipConfig = (relationshipId: string, config: RelationshipConfig) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema) {
      console.error('No schema selected or database not loaded');
      return;
    }

    const currentRelationship = findRelationshipInSchema(selectedSchema, relationshipId);
    if (!currentRelationship) {
      console.error('Relationship not found');
      return;
    }

    const typeConfig = RELATIONSHIP_TYPES[config.type];
    const newKind = config.isNonIdentifying ? 'NON_IDENTIFYING' : 'IDENTIFYING';
    const currentExtra = (currentRelationship.extra || {}) as RelationshipExtra;

    const needsRecreation =
      currentRelationship.kind !== newKind || currentRelationship.cardinality !== typeConfig.cardinality;

    if (needsRecreation) {
      erdStore.deleteRelationship(selectedSchemaId, relationshipId);
      erdStore.createRelationship(selectedSchemaId, {
        ...currentRelationship,
        kind: newKind,
        cardinality: typeConfig.cardinality,
        extra: {
          ...currentExtra,
          controlPointX: config.controlPointX,
          controlPointY: config.controlPointY,
        },
      });
    } else {
      const newExtra = {
        ...currentExtra,
        controlPointX: config.controlPointX,
        controlPointY: config.controlPointY,
      };

      if (JSON.stringify(currentExtra) !== JSON.stringify(newExtra)) {
        erdStore.updateRelationshipExtra(selectedSchemaId, relationshipId, newExtra);
      }
    }
  };

  const deleteRelationship = (relationshipId: string) => {
    const selectedSchemaId = erdStore.selectedSchemaId;

    if (!selectedSchemaId) {
      console.error('No schema selected');
      return;
    }

    erdStore.deleteRelationship(selectedSchemaId, relationshipId);
    setSelectedRelationship(null);
  };

  const changeRelationshipName = (relationshipId: string, newName: string) => {
    const selectedSchemaId = erdStore.selectedSchemaId;

    if (!selectedSchemaId) {
      console.error('No schema selected');
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
