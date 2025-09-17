export const FIELD_TYPES = [
  'VARCHAR',
  'INT',
  'BIGINT',
  'TEXT',
  'DATETIME',
  'BOOLEAN',
  'DECIMAL',
  'JSON',
];

export const RELATIONSHIP_TYPES = {
  'one-to-one': {
    label: '1:1',
    markerEnd: 'arrowclosed',
    style: { strokeWidth: 2 },
  },
  'one-to-many': {
    label: '1:N',
    markerEnd: 'arrowclosed',
    style: { strokeWidth: 2 },
  },
  'many-to-many': {
    label: 'N:M',
    markerEnd: 'arrowclosed',
    style: { strokeWidth: 2, strokeDasharray: '5,5' },
  },
};
