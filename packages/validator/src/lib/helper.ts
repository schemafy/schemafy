import { ulid } from 'ulid';
import { Column, Constraint, Index, Relationship, RelationshipColumn, Schema, Table } from '..';

export const detectCircularReference = (
  schema: Schema,
  fromTableId: Table['id'],
  toTableId: Table['id'],
  visited: Set<Table['id']> = new Set()
): boolean => {
  if (visited.has(fromTableId)) return true;
  if (fromTableId === toTableId) return true;

  visited.add(fromTableId);

  const referencedTables = new Set<Table['id']>();
  schema.tables.forEach((table) => {
    table.relationships.forEach((rel) => {
      if (rel.srcTableId === fromTableId) {
        referencedTables.add(rel.tgtTableId);
      }
    });
  });

  for (const referencedTableId of referencedTables) {
    if (detectCircularReference(schema, referencedTableId, toTableId, new Set(visited))) {
      return true;
    }
  }

  return false;
};

export const isValidColumnName = (str: string): boolean => {
  if (/-/.test(str)) {
    return false;
  }

  if (/\s/.test(str)) {
    return false;
  }

  if (/^[0-9]/.test(str)) {
    return false;
  }

  if (/[^a-zA-Z0-9]/.test(str)) {
    return false;
  }

  return true;
};

export const categorizedMysqlDataTypes = [
  'TINYINT',
  'SMALLINT',
  'MEDIUMINT',
  'INT',
  'INTEGER',
  'BIGINT',
  'FLOAT',
  'DOUBLE',
  'REAL',
  'DECIMAL',
  'NUMERIC',
  'BIT',
  'BOOL',
  'BOOLEAN',
  'CHAR',
  'VARCHAR',
  'TINYTEXT',
  'TEXT',
  'MEDIUMTEXT',
  'LONGTEXT',
  'BINARY',
  'VARBINARY',
  'BLOB',
  'TINYBLOB',
  'MEDIUMBLOB',
  'LONGBLOB',
  'ENUM',
  'SET',
  'DATE',
  'TIME',
  'DATETIME',
  'TIMESTAMP',
  'YEAR',
  'GEOMETRY',
  'POINT',
  'LINESTRING',
  'POLYGON',
  'MULTIPOINT',
  'MULTILINESTRING',
  'MULTIPOLYGON',
  'GEOMETRYCOLLECTION',
  'JSON',
];

export const propagateNewPrimaryKey = (
  currentSchema: Schema,
  parentTableId: Table['id'],
  newPkColumn: Column,
  visited: Set<string> = new Set()
): Schema => {
  if (visited.has(parentTableId)) return currentSchema;
  visited.add(parentTableId);

  let updatedSchema = currentSchema;

  for (const table of updatedSchema.tables) {
    for (const rel of table.relationships) {
      if (rel.tgtTableId !== parentTableId) continue;

      const childTable = updatedSchema.tables.find((t) => t.id === rel.srcTableId);
      if (!childTable) continue;

      const parentTable = updatedSchema.tables.find((t) => t.id === parentTableId)!;
      const existingColumn = childTable.columns.find((c) => c.id.startsWith(`col_${parentTable.id}_${newPkColumn.id}`));

      if (existingColumn) continue;

      const newFkColumnId = `col_${parentTable.id}_${newPkColumn.id}_${ulid()}`;
      const columnName = `${parentTable.name}_${newPkColumn.name}`;

      const newFkColumn: Column = {
        ...newPkColumn,
        isAffected: true,
        id: newFkColumnId,
        tableId: childTable.id,
        name: columnName,
        ordinalPosition: childTable.columns.length + 1,
        createdAt: new Date(),
        updatedAt: new Date(),
      };

      updatedSchema = {
        ...updatedSchema,
        isAffected: true,
        tables: updatedSchema.tables.map((t) =>
          t.id === childTable.id
            ? {
                ...t,
                isAffected: true,
                columns: [...t.columns, newFkColumn],
              }
            : t
        ),
      };

      const newRelColumn: RelationshipColumn = {
        id: `rel_col_${rel.id}_${newFkColumnId}_${ulid()}`,
        relationshipId: rel.id,
        fkColumnId: newFkColumnId,
        refColumnId: newPkColumn.id,
        seqNo: rel.columns.length + 1,
        isAffected: true,
      };

      const updateRelationships: Relationship[] = parentTable.relationships.map((r) =>
        r.id === rel.id
          ? {
              ...r,
              isAffected: true,
              columns: [...r.columns, newRelColumn],
            }
          : r
      );

      const updateTables: Table[] = updatedSchema.tables.map((t) =>
        t.id === parentTableId
          ? {
              ...t,
              isAffected: true,
              relationships: updateRelationships,
            }
          : t
      );

      updatedSchema = {
        ...updatedSchema,
        isAffected: true,
        tables: updateTables,
      };

      if (rel.kind === 'IDENTIFYING') {
        updatedSchema = propagateNewPrimaryKey(
          structuredClone(updatedSchema),
          rel.tgtTableId,
          newFkColumn,
          new Set(visited)
        );
      }
    }
  }

  return updatedSchema;
};

export const deleteCascadingForeignKeys = (
  currentSchema: Schema,
  parentTableId: Table['id'],
  deletedPkColumnId: Column['id'],
  visited: Set<string> = new Set()
): Schema => {
  if (visited.has(parentTableId)) return currentSchema;
  visited.add(parentTableId);

  let updatedSchema = currentSchema;

  for (const table of updatedSchema.tables) {
    for (const rel of table.relationships) {
      if (rel.srcTableId !== parentTableId) continue;

      const childTable = updatedSchema.tables.find((t) => t.id === rel.srcTableId);

      if (!childTable) continue;

      const fkColumnsToDelete = rel.columns
        .filter((relCol) => relCol.refColumnId === deletedPkColumnId)
        .map((relCol) => relCol.fkColumnId);

      if (fkColumnsToDelete.length === 0) continue;

      const updateIndexes: Index[] = childTable.indexes.map((idx) => ({
        ...idx,
        isAffected: idx.columns.some((ic) => fkColumnsToDelete.includes(ic.columnId)),
        columns: idx.columns.filter((ic) => !fkColumnsToDelete.includes(ic.columnId)),
      }));

      const updateConstraints: Constraint[] = childTable.constraints.map((constraint) => ({
        ...constraint,
        isAffected: constraint.columns.some((cc) => fkColumnsToDelete.includes(cc.columnId)),
        columns: constraint.columns.filter((cc) => !fkColumnsToDelete.includes(cc.columnId)),
      }));

      const updateRelationships: Relationship[] = childTable.relationships.map((relationship) => ({
        ...relationship,
        isAffected: relationship.columns.some(
          (rc) => fkColumnsToDelete.includes(rc.fkColumnId) || fkColumnsToDelete.includes(rc.refColumnId)
        ),
        columns: relationship.columns.filter(
          (rc) => !fkColumnsToDelete.includes(rc.fkColumnId) && !fkColumnsToDelete.includes(rc.refColumnId)
        ),
      }));

      const updateTables: Table[] = updatedSchema.tables.map((t) =>
        t.id === childTable.id
          ? {
              ...t,
              isAffected: true,
              columns: t.columns.filter((col) => !fkColumnsToDelete.includes(col.id)),
              indexes: updateIndexes,
              constraints: updateConstraints,
              relationships: updateRelationships,
            }
          : t
      );

      updatedSchema = {
        ...updatedSchema,
        isAffected: true,
        tables: updateTables,
      };

      if (rel.kind === 'IDENTIFYING') {
        for (const fkColumnId of fkColumnsToDelete) {
          updatedSchema = deleteCascadingForeignKeys(
            structuredClone(updatedSchema),
            rel.tgtTableId,
            fkColumnId,
            new Set(visited)
          );
        }
      }
    }
  }

  return updatedSchema;
};
