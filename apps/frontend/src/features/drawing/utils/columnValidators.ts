import type { Schema } from '@schemafy/validator';

export function isForeignKeyColumn(
  schema: Schema,
  tableId: string,
  columnId: string,
): boolean {
  return schema.tables.some((table) =>
    table.relationships.some((relationship) => {
      if (relationship.fkTableId === tableId) {
        return relationship.columns.some((col) => col.fkColumnId === columnId);
      }
      return false;
    }),
  );
}
