export const RELATIONSHIP_TYPES = {
  'one-to-one': {
    label: '1:1',
    style: { stroke: 'var(--color-schemafy-dark-gray)', strokeWidth: 2 },
    markerStart: 'erd-one-start',
    markerEnd: 'erd-one-end',
  },
  'one-to-many': {
    label: '1:N',
    style: { stroke: 'var(--color-schemafy-dark-gray)', strokeWidth: 2 },
    markerStart: 'erd-one-start',
    markerEnd: 'erd-many-end',
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
  isDashed: boolean;
}

export const isRelationshipType = (
  value: unknown,
): value is RelationshipType => {
  return (
    typeof value === 'string' &&
    (Object.keys(RELATIONSHIP_TYPES) as string[]).includes(value)
  );
};
