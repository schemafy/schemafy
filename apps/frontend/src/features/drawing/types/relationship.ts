import type { Point } from './index';

export const RELATIONSHIP_TYPES = {
  'one-to-one': {
    label: '1:1',
    cardinality: 'ONE_TO_ONE',
    style: { stroke: 'var(--color-schemafy-dark-gray)', strokeWidth: 2 },
    markerStart: 'erd-one-start',
    markerEnd: 'erd-one-end',
  },
  'one-to-many': {
    label: '1:N',
    cardinality: 'ONE_TO_MANY',
    style: { stroke: 'var(--color-schemafy-dark-gray)', strokeWidth: 2 },
    markerStart: 'erd-many-start',
    markerEnd: 'erd-one-end',
  },
} as const;

export const RELATIONSHIP_STYLE_TYPES = {
  solid: {
    stroke: 'var(--color-schemafy-dark-gray)',
    strokeWidth: 2,
  },
  dashed: {
    stroke: 'var(--color-schemafy-dark-gray)',
    strokeWidth: 2,
    strokeDasharray: '5 5',
  },
} as const;

export type RelationshipType = keyof typeof RELATIONSHIP_TYPES;

export interface RelationshipConfig {
  type: RelationshipType;
  isNonIdentifying: boolean;
}

export const isRelationshipType = (
  value: unknown,
): value is RelationshipType => {
  return (
    typeof value === 'string' &&
    (Object.keys(RELATIONSHIP_TYPES) as string[]).includes(value)
  );
};

export type CrossDirectionControlPoints = {
  controlPoint1: Point;
  controlPoint2: Point;
};

export type SameDirectionControlPoints = {
  controlPoint1: Point;
};

export type RelationshipExtra = {
  fkHandle?: string;
  pkHandle?: string;
} & Partial<CrossDirectionControlPoints>;

export interface EdgeData extends Record<string, unknown> {
  relationshipType: RelationshipType;
  isNonIdentifying: boolean;
  controlPoint1?: Point;
  controlPoint2?: Point;
  onControlPointDragEnd?: (
    id: string,
    controlPoint1: Point,
    controlPoint2?: Point,
  ) => void;
}

export type ValidationSuccess = {
  isValid: true;
  fkTableId: string;
  pkTableId: string;
  pkColumnIds: string[];
};

export type ValidationFailure = {
  isValid: false;
  error?: string;
};

export type ConnectionValidationResult = ValidationSuccess | ValidationFailure;
