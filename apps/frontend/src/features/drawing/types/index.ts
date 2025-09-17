export const RELATIONSHIP_TYPES = {
  'one-to-one': {
    label: '1:1',
    style: { stroke: '#059669', strokeWidth: 2 },
    markerStart: 'erd-one-start',
    markerEnd: 'erd-one-end',
  },
  'one-to-many': {
    label: '1:N',
    style: { stroke: '#2563eb', strokeWidth: 2 },
    markerStart: 'erd-one-start',
    markerEnd: 'erd-many-end',
  },
  'many-to-many': {
    label: 'N:M',
    style: { stroke: '#dc2626', strokeWidth: 2 },
    markerStart: 'erd-many-start',
    markerEnd: 'erd-many-end',
  },
} as const;

export type RelationshipType = keyof typeof RELATIONSHIP_TYPES;
