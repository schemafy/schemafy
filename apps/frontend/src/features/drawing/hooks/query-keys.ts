export const erdKeys = {
  all: ['erd'] as const,
  schemaSnapshots: (schemaId: string) =>
    [...erdKeys.all, 'schemaSnapshots', schemaId] as const,
};
