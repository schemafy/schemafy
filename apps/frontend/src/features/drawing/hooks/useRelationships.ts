import type { Connection, Edge, EdgeChange } from '@xyflow/react';
import { useState, useRef } from 'react';
import { ErdStore } from '@/store';
import type { RelationshipConfig } from '../types';
import { RELATIONSHIP_TYPES } from '../types';
import {
  convertRelationshipsToEdges,
  createRelationshipFromConnection,
  findRelationshipInSchema,
} from '../utils/relationshipHelpers';

export const useRelationships = (relationshipConfig: RelationshipConfig) => {
  const erdStore = ErdStore.getInstance();
  const [selectedRelationship, setSelectedRelationship] = useState<string | null>(null);
  const relationshipReconnectSuccessful = useRef(true);

  const getRelationships = (): Edge[] => {
    const { erdState, selectedSchemaId } = erdStore;

    if (erdState.state !== 'loaded' || !selectedSchemaId) {
      return [];
    }

    const schema = erdState.database.schemas.find((s) => s.id === selectedSchemaId);
    if (!schema) {
      return [];
    }

    return convertRelationshipsToEdges(schema);
  };

  const relationships = getRelationships();

  const onConnect = (params: Connection) => {
    const { selectedSchemaId, erdState, createRelationship } = erdStore;

    if (!selectedSchemaId || !params.source || !params.target || erdState.state !== 'loaded') {
      return;
    }

    const schema = erdState.database.schemas.find((s) => s.id === selectedSchemaId);
    if (!schema) return;

    const newRelationship = createRelationshipFromConnection({
      schema,
      connection: params,
      relationshipConfig,
    });

    if (newRelationship) {
      createRelationship(selectedSchemaId, newRelationship);
    }
  };

  const onRelationshipsChange = (changes: EdgeChange[]) => {
    const { selectedSchemaId, deleteRelationship } = erdStore;

    if (!selectedSchemaId) return;

    changes.forEach((change) => {
      if (change.type === 'remove') {
        deleteRelationship(selectedSchemaId, change.id);
      }
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
    const { selectedSchemaId, erdState, deleteRelationship, createRelationship } = erdStore;

    if (!selectedSchemaId || erdState.state !== 'loaded') return;

    relationshipReconnectSuccessful.current = true;
    deleteRelationship(selectedSchemaId, oldRelationship.id);

    const schema = erdState.database.schemas.find((s) => s.id === selectedSchemaId);
    if (!schema) return;

    const newRelationship = createRelationshipFromConnection({
      schema,
      connection: newConnection,
      relationshipConfig,
    });

    if (newRelationship) {
      createRelationship(selectedSchemaId, newRelationship);
    }
  };

  const onReconnectEnd = (_: MouseEvent | TouchEvent, relationship: Edge) => {
    const { selectedSchemaId, deleteRelationship } = erdStore;

    if (!selectedSchemaId) return;

    if (!relationshipReconnectSuccessful.current) {
      deleteRelationship(selectedSchemaId, relationship.id);
    }
    relationshipReconnectSuccessful.current = true;
  };

  const updateRelationshipConfig = (relationshipId: string, config: RelationshipConfig) => {
    const { selectedSchemaId, erdState } = erdStore;

    if (!selectedSchemaId || erdState.state !== 'loaded') {
      console.error('No schema selected or database not loaded');
      return;
    }

    const schema = erdState.database.schemas.find((s) => s.id === selectedSchemaId);
    if (!schema) return;

    const currentRelationship = findRelationshipInSchema(schema, relationshipId);
    if (!currentRelationship) {
      console.error('Relationship not found');
      return;
    }

    const typeConfig = RELATIONSHIP_TYPES[config.type];
    const newKind = config.isNonIdentifying ? 'NON_IDENTIFYING' : 'IDENTIFYING';
    const currentExtra = (currentRelationship.extra || {}) as {
      sourceHandle?: string;
      targetHandle?: string;
      controlPointX?: number;
      controlPointY?: number;
    };

    const needsRecreation =
      currentRelationship.kind !== newKind || currentRelationship.cardinality !== typeConfig.cardinality;

    if (needsRecreation) {
      const { deleteRelationship: deleteRel, createRelationship } = erdStore;

      deleteRel(selectedSchemaId, relationshipId);
      createRelationship(selectedSchemaId, {
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
    const { selectedSchemaId, deleteRelationship: deleteRel } = erdStore;

    if (!selectedSchemaId) {
      console.error('No schema selected');
      return;
    }
    deleteRel(selectedSchemaId, relationshipId);
    setSelectedRelationship(null);
  };

  const changeRelationshipName = (relationshipId: string, newName: string) => {
    const { selectedSchemaId, changeRelationshipName: changeName } = erdStore;

    if (!selectedSchemaId) {
      console.error('No schema selected');
      return;
    }
    changeName(selectedSchemaId, relationshipId, newName);
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
