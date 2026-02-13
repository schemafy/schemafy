export const erdKeys = {
  all: ['erd'] as const,
  schemas: (projectId: string) =>
    [...erdKeys.all, 'schemas', projectId] as const,
  schemaSnapshots: (schemaId: string) =>
    [...erdKeys.all, 'schemaSnapshots', schemaId] as const,
};
