import type { Connection, Edge } from '@xyflow/react';
import { ulid } from 'ulid';
import type { Relationship, Schema } from '@schemafy/validator';
import {
  RELATIONSHIP_TYPES,
  RELATIONSHIP_STYLE_TYPES,
  type RelationshipConfig,
  type RelationshipType,
} from '../types';

export const getRelationshipType = (cardinality: string): RelationshipType => {
  return cardinality === '1:N' ? 'one-to-many' : 'one-to-one';
};

export const getRelationshipStyle = (isNonIdentifying: boolean) => {
  return isNonIdentifying
    ? RELATIONSHIP_STYLE_TYPES.dashed
    : RELATIONSHIP_STYLE_TYPES.solid;
};

export const findRelationshipInSchema = (
  schema: Schema,
  relationshipId: string,
): Relationship | undefined => {
  for (const table of schema.tables) {
    const relationship = table.relationships.find(
      (r) => r.id === relationshipId,
    );
    if (relationship) return relationship;
  }
  return undefined;
};

interface CreateRelationshipParams {
  schema: Schema;
  connection: Connection;
  relationshipConfig: RelationshipConfig;
}

export const createRelationshipFromConnection = ({
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

  const targetPk = targetTable.constraints.find(
    (c) => c.kind === 'PRIMARY_KEY',
  );

  if (!targetPk || targetPk.columns.length === 0) {
    console.error(
      'Target table must have a primary key to create a relationship',
    );
    return null;
  }

  const targetPkColumnIds = targetPk.columns.map((c) => c.columnId);

  const relId = ulid();
  const typeConfig = RELATIONSHIP_TYPES[relationshipConfig.type];
  const kind = relationshipConfig.isNonIdentifying
    ? 'NON_IDENTIFYING'
    : 'IDENTIFYING';

  const relationshipColumns = targetPkColumnIds.map((pkColId, index) => {
    return {
      id: ulid(),
      relationshipId: relId,
      fkColumnId: ulid(),
      refColumnId: pkColId,
      seqNo: index + 1,
      isAffected: false,
    };
  });

  return {
    id: relId,
    srcTableId: connection.source,
    tgtTableId: connection.target,
    name: `${sourceTable.name}_${targetTable.name}`,
    kind,
    cardinality: typeConfig.cardinality,
    onDelete: 'CASCADE',
    onUpdate: 'CASCADE',
    fkEnforced: false,
    columns: relationshipColumns,
    isAffected: false,
    extra: {
      sourceHandle: connection.sourceHandle,
      targetHandle: connection.targetHandle,
      controlPointX: relationshipConfig.controlPointX,
      controlPointY: relationshipConfig.controlPointY,
    },
  };
};

export type RelationshipExtra = {
  sourceHandle?: string;
  targetHandle?: string;
  controlPointX?: number;
  controlPointY?: number;
};

export const shouldRecreateRelationship = (
  currentRelationship: Relationship,
  newKind: string,
  newCardinality: string,
): boolean => {
  return (
    currentRelationship.kind !== newKind ||
    currentRelationship.cardinality !== newCardinality
  );
};

export const mergeRelationshipExtra = (
  current: RelationshipExtra,
  config: RelationshipConfig,
): RelationshipExtra => {
  return {
    ...current,
    controlPointX: config.controlPointX,
    controlPointY: config.controlPointY,
  };
};

export const hasExtraChanged = (
  current: RelationshipExtra,
  updated: RelationshipExtra,
): boolean => {
  return JSON.stringify(current) !== JSON.stringify(updated);
};

export const convertRelationshipsToEdges = (schema: Schema): Edge[] => {
  const allRelationships: Edge[] = [];

  schema.tables.forEach((table) => {
    table.relationships.forEach((rel) => {
      const relationshipType = getRelationshipType(rel.cardinality);
      const baseConfig = RELATIONSHIP_TYPES[relationshipType];
      const isNonIdentifying = rel.kind === 'NON_IDENTIFYING';
      const style = getRelationshipStyle(isNonIdentifying);
      const extra = (rel.extra || {}) as RelationshipExtra;

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
        label: rel.name,
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
};
