import type { Connection, Edge } from '@xyflow/react';
import type {
  TableSnapshotResponse,
  RelationshipSnapshotResponse,
  ConstraintSnapshotResponse,
} from '../api';
import {
  RELATIONSHIP_TYPES,
  RELATIONSHIP_STYLE_TYPES,
  type RelationshipType,
  type RelationshipExtra,
} from '../types';

export const getRelationshipType = (cardinality: string): RelationshipType => {
  return cardinality === '1:N' ? 'one-to-many' : 'one-to-one';
};

export const getRelationshipStyle = (isNonIdentifying: boolean) => {
  return isNonIdentifying
    ? RELATIONSHIP_STYLE_TYPES.dashed
    : RELATIONSHIP_STYLE_TYPES.solid;
};

export const convertRelationshipSnapshotToEdge = (
  snapshot: RelationshipSnapshotResponse,
): Edge => {
  const { relationship } = snapshot;
  const relationshipType = getRelationshipType(relationship.cardinality);
  const baseConfig = RELATIONSHIP_TYPES[relationshipType];
  const isNonIdentifying = relationship.kind === 'NON_IDENTIFYING';
  const style = getRelationshipStyle(isNonIdentifying);

  let extra: RelationshipExtra = {};
  if (relationship.extra) {
    try {
      extra = JSON.parse(relationship.extra) as RelationshipExtra;
    } catch {
      extra = {};
    }
  }

  return {
    id: relationship.id,
    source: relationship.fkTableId,
    target: relationship.pkTableId,
    sourceHandle: extra.sourceHandle,
    targetHandle: extra.targetHandle,
    type: 'customSmoothStep',
    style,
    markerStart: baseConfig.markerStart,
    markerEnd: baseConfig.markerEnd,
    label: relationship.name,
    labelStyle: {
      fontSize: 12,
      fontWeight: 'bold',
      color: style.stroke,
    },
    data: {
      relationshipType,
      isNonIdentifying,
      controlPoint1: extra.controlPoint1,
      controlPoint2: extra.controlPoint2,
      relationshipSnapshot: snapshot,
    },
  } as Edge;
};

export const convertSnapshotsToEdges = (
  snapshots: Record<string, TableSnapshotResponse>,
): Edge[] => {
  const seenRelationshipIds = new Set<string>();

  return Object.values(snapshots).flatMap((snapshot) =>
    snapshot.relationships
      .filter((relSnapshot) => {
        if (seenRelationshipIds.has(relSnapshot.relationship.id)) {
          return false;
        }
        seenRelationshipIds.add(relSnapshot.relationship.id);
        return true;
      })
      .map((relSnapshot) => convertRelationshipSnapshotToEdge(relSnapshot)),
  );
};

export const shouldRecreateRelationship = (
  currentKind: string,
  currentCardinality: string,
  newKind: string,
  newCardinality: string,
): boolean => {
  return currentKind !== newKind || currentCardinality !== newCardinality;
};

export const findRelationshipById = (
  snapshots: Record<string, TableSnapshotResponse>,
  relationshipId: string,
): RelationshipSnapshotResponse | undefined => {
  return Object.values(snapshots)
    .flatMap((s) => s.relationships)
    .find((r) => r.relationship.id === relationshipId);
};

export const parseRelationshipExtra = (
  extraString: string | null,
): RelationshipExtra => {
  if (!extraString) return {};

  try {
    return JSON.parse(extraString) as RelationshipExtra;
  } catch {
    return {};
  }
};

interface ValidateConnectionParams {
  snapshots: Record<string, TableSnapshotResponse>;
  connection: Connection;
}

const findPrimaryKey = (snapshot: TableSnapshotResponse) =>
  snapshot.constraints.find((c) => c.constraint.kind === 'PRIMARY_KEY');

const hasValidPrimaryKey = (pk: ConstraintSnapshotResponse | undefined) =>
  pk && pk.columns.length > 0;

export const validateConnection = ({
  snapshots,
  connection,
}: ValidateConnectionParams): {
  isValid: boolean;
  error?: string;
  fkTableId?: string;
  pkTableId?: string;
  pkColumnIds?: string[];
} => {
  if (!connection.source || !connection.target) {
    return { isValid: false, error: 'Invalid connection' };
  }

  const sourceSnapshot = snapshots[connection.source];
  const targetSnapshot = snapshots[connection.target];

  if (!sourceSnapshot || !targetSnapshot) {
    return { isValid: false, error: 'Table not found' };
  }

  const sourcePk = findPrimaryKey(sourceSnapshot);
  const targetPk = findPrimaryKey(targetSnapshot);

  if (!hasValidPrimaryKey(targetPk) && !hasValidPrimaryKey(sourcePk)) {
    return {
      isValid: false,
      error: 'At least one table must have a primary key',
    };
  }

  const targetHasPk = hasValidPrimaryKey(targetPk);
  const pkTableId = targetHasPk ? connection.target : connection.source;
  const fkTableId = targetHasPk ? connection.source : connection.target;
  const pkColumnIds = (targetHasPk ? targetPk : sourcePk)!.columns.map(
    (c) => c.columnId,
  );

  return {
    isValid: true,
    fkTableId,
    pkTableId,
    pkColumnIds,
  };
};
