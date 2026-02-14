export const getColumnName = (
  columns: Array<{ id: string; name: string }>,
  columnId: string,
): string => {
  return columns.find((col) => col.id === columnId)?.name || 'Unknown';
};
