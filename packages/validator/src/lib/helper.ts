import { Column, Relationship, Schema, Table } from '..';

export const findRelationshipsByTable = (
  schema: Schema,
  tableId: string
): { table: Table; relationships: Relationship[] }[] => {
  return schema.tables
    .map((table) => ({
      table,
      relationships: table.relationships.filter((rel) => rel.srcTableId === tableId || rel.tgtTableId === tableId),
    }))
    .filter((result) => result.relationships.length > 0);
};

export const findRelationshipsByColumn = (
  schema: Schema,
  columnId: string
): { table: Table; relationships: Relationship[] }[] => {
  return schema.tables
    .map((table) => ({
      table,
      relationships: table.relationships.filter((rel) =>
        rel.columns.some((rc) => rc.srcColumnId === columnId || rc.tgtColumnId === columnId)
      ),
    }))
    .filter((result) => result.relationships.length > 0);
};

export const isColumnNullable = (table: Table, columnId: string): boolean => {
  // Check if the column has a NOT_NULL constraint
  const notNullConstraint = table.constraints.find(
    (constraint) => constraint.kind === 'NOT_NULL' && constraint.columns.some((cc) => cc.columnId === columnId)
  );
  return !notNullConstraint;
};

export const shouldUpdateCardinalityForNullChange = (
  relationship: Relationship,
  changedColumnId: string,
  newNullable: boolean
): boolean => {
  // Check if the changed column is part of this relationship and affects cardinality
  const relColumn = relationship.columns.find(
    (rc) => rc.srcColumnId === changedColumnId || rc.tgtColumnId === changedColumnId
  );
  if (!relColumn) return false;

  // If FK column becomes nullable, 1:1 might need to become 1:N
  if (newNullable && relationship.cardinality === '1:1') {
    return true;
  }

  return false;
};

export const detectCircularReference = (
  schema: Schema,
  fromTableId: string,
  toTableId: string,
  visited: Set<string> = new Set()
): boolean => {
  if (visited.has(fromTableId)) return true;
  if (fromTableId === toTableId) return true;

  visited.add(fromTableId);

  // Find all tables that this table references
  const referencedTables = new Set<string>();
  schema.tables.forEach((table) => {
    table.relationships.forEach((rel) => {
      if (rel.srcTableId === fromTableId) {
        referencedTables.add(rel.tgtTableId);
      }
    });
  });

  // Recursively check for cycles
  for (const referencedTableId of referencedTables) {
    if (detectCircularReference(schema, referencedTableId, toTableId, new Set(visited))) {
      return true;
    }
  }

  return false;
};

export const validateColumnTypeCompatibility = (srcColumn: Column, tgtColumn: Column): boolean => {
  // Simplified type compatibility check
  // In a real implementation, this would be more sophisticated
  if (srcColumn.dataType !== tgtColumn.dataType) return false;
  if (srcColumn.lengthScale !== tgtColumn.lengthScale) return false;
  return true;
};
