import { type Connection, type Edge, type EdgeChange } from '@xyflow/react';
import { useState, useRef, useMemo, useCallback } from 'react';
import { ulid } from 'ulid';
import { ErdStore } from '@/store';
import type { Relationship, Schema } from '@schemafy/validator';
import { RELATIONSHIP_TYPES, RELATIONSHIP_STYLE_TYPES, type RelationshipConfig, type RelationshipType } from '../types';

const getRelationshipType = (cardinality: string): RelationshipType => {
  return cardinality === '1:N' ? 'one-to-many' : 'one-to-one';
};

const getRelationshipStyle = (isNonIdentifying: boolean) => {
  return isNonIdentifying ? RELATIONSHIP_STYLE_TYPES.dashed : RELATIONSHIP_STYLE_TYPES.solid;
};

interface CreateRelationshipParams {
  schema: Schema;
  connection: Connection;
  relationshipConfig: RelationshipConfig;
}

const createRelationshipFromConnection = ({
  schema,
  connection,
  relationshipConfig,
}: CreateRelationshipParams): Relationship | null => {
  if (!connection.source || !connection.target) {
    return null;
  }

  const sourceTable = schema.tables.find((t) => t.id === connection.source);
  if (!sourceTable) {
    console.error('Source table not found');
    return null;
  }

  const targetTable = schema.tables.find((t) => t.id === connection.target);
  if (!targetTable) {
    console.error('Target table not found');
    return null;
  }

  const targetPk = targetTable.constraints.find((c) => c.kind === 'PRIMARY_KEY');
  const targetPkColId = targetPk?.columns[0]?.columnId;

  if (!targetPkColId) {
    console.error('Target table must have a primary key to create a relationship');
    return null;
  }

  const relId = ulid();
  const typeConfig = RELATIONSHIP_TYPES[relationshipConfig.type];
  const kind = relationshipConfig.isNonIdentifying ? 'NON_IDENTIFYING' : 'IDENTIFYING';
  const fkColumnId = ulid();

  return {
    id: relId,
    srcTableId: connection.source,
    tgtTableId: connection.target,
    name: `fk_${connection.source}_${connection.target}`,
    kind,
    cardinality: typeConfig.cardinality,
    onDelete: 'CASCADE',
    onUpdate: 'CASCADE',
    fkEnforced: false,
    columns: [
      {
        id: ulid(),
        relationshipId: relId,
        fkColumnId,
        refColumnId: targetPkColId,
        seqNo: 1,
      },
    ],
    extra: {
      sourceHandle: connection.sourceHandle,
      targetHandle: connection.targetHandle,
      controlPointX: relationshipConfig.controlPointX,
      controlPointY: relationshipConfig.controlPointY,
    },
  };
};

const findRelationshipInSchema = (schema: Schema, relationshipId: string): Relationship | undefined => {
  for (const table of schema.tables) {
    const relationship = table.relationships.find((r) => r.id === relationshipId);
    if (relationship) return relationship;
  }
  return undefined;
};

export const useRelationships = (relationshipConfig: RelationshipConfig) => {
  const erdStore = ErdStore.getInstance();
  const [selectedRelationship, setSelectedRelationship] = useState<string | null>(null);
  const relationshipReconnectSuccessful = useRef(true);

  const relationships = useMemo<Edge[]>(() => {
    const { erdState, selectedSchemaId } = erdStore;

    if (erdState.state !== 'loaded' || !selectedSchemaId) {
      return [];
    }

    const schema = erdState.database.schemas.find((s) => s.id === selectedSchemaId);
    if (!schema) {
      return [];
    }

    const allRelationships: Edge[] = [];

    schema.tables.forEach((table) => {
      table.relationships.forEach((rel) => {
        const relationshipType = getRelationshipType(rel.cardinality);
        const baseConfig = RELATIONSHIP_TYPES[relationshipType];
        const isNonIdentifying = rel.kind === 'NON_IDENTIFYING';
        const style = getRelationshipStyle(isNonIdentifying);
        const extra = (rel.extra || {}) as {
          sourceHandle?: string;
          targetHandle?: string;
          controlPointX?: number;
          controlPointY?: number;
        };

        const edge: Edge = {
          id: rel.id,
          source: rel.srcTableId,
          target: rel.tgtTableId,
          sourceHandle: extra.sourceHandle,
          targetHandle: extra.targetHandle,
          type: 'customSmoothStep',
          style,
          markerStart: baseConfig.markerStart,
          markerEnd: baseConfig.markerEnd,
          label: baseConfig.label,
          labelStyle: {
            fontSize: 12,
            fontWeight: 'bold',
            color: style.stroke,
          },
          data: {
            relationshipType,
            isNonIdentifying,
            controlPointX: extra.controlPointX,
            controlPointY: extra.controlPointY,
            dbRelationship: rel,
          },
        } as Edge;

        allRelationships.push(edge);
      });
    });

    return allRelationships;
  }, [erdStore.erdState, erdStore.selectedSchemaId]);

  const onConnect = useCallback(
    (params: Connection) => {
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
    },
    [erdStore, relationshipConfig],
  );

  const onRelationshipsChange = useCallback(
    (changes: EdgeChange[]) => {
      const { selectedSchemaId, deleteRelationship } = erdStore;

      if (!selectedSchemaId) return;

      changes.forEach((change) => {
        if (change.type === 'remove') {
          deleteRelationship(selectedSchemaId, change.id);
        }
      });
    },
    [erdStore],
  );

  const onRelationshipClick = useCallback((event: React.MouseEvent, relationship: Edge) => {
    event.stopPropagation();
    setSelectedRelationship(relationship.id);
  }, []);

  const onReconnectStart = useCallback(() => {
    relationshipReconnectSuccessful.current = false;
  }, []);

  const onReconnect = useCallback(
    (oldRelationship: Edge, newConnection: Connection) => {
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
    },
    [erdStore, relationshipConfig],
  );

  const onReconnectEnd = useCallback(
    (_: MouseEvent | TouchEvent, relationship: Edge) => {
      const { selectedSchemaId, deleteRelationship } = erdStore;

      if (!selectedSchemaId) return;

      if (!relationshipReconnectSuccessful.current) {
        deleteRelationship(selectedSchemaId, relationship.id);
      }
      relationshipReconnectSuccessful.current = true;
    },
    [erdStore],
  );

  const updateRelationshipConfig = useCallback(
    (relationshipId: string, config: RelationshipConfig) => {
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
    },
    [erdStore],
  );

  const deleteRelationship = useCallback(
    (relationshipId: string) => {
      const { selectedSchemaId, deleteRelationship: deleteRel } = erdStore;

      if (!selectedSchemaId) {
        console.error('No schema selected');
        return;
      }
      deleteRel(selectedSchemaId, relationshipId);
      setSelectedRelationship(null);
    },
    [erdStore],
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
    setSelectedRelationship,
  };
};
