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

export const precisionRequired = ['DECIMAL', 'NUMERIC'];
export const lengthScaleRequired = ['VARCHAR', 'CHAR'];

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
          rel.srcTableId,
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
      if (rel.tgtTableId !== parentTableId) continue;

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

export const propagateKeysToChildren = (
  currentSchema: Schema,
  parentTableId: Table['id'],
  visited: Set<Table['id']> = new Set()
): Schema => {
  if (visited.has(parentTableId)) return currentSchema;
  visited.add(parentTableId);

  const parentTable = currentSchema.tables.find((t) => t.id === parentTableId);
  if (!parentTable) return currentSchema;

  const pkConstraint = parentTable.constraints.find((c) => c.kind === 'PRIMARY_KEY');
  if (!pkConstraint) return currentSchema;

  const pkColumnIds = pkConstraint.columns.map((cc) => cc.columnId);
  const pkColumns = parentTable.columns.filter((col) => pkColumnIds.includes(col.id));

  let updatedSchema = currentSchema;

  for (const table of updatedSchema.tables) {
    for (const rel of table.relationships) {
      if (rel.tgtTableId !== parentTableId) continue;

      const childTable = updatedSchema.tables.find((t) => t.id === rel.srcTableId);
      if (!childTable) continue;

      let updatedChildTable = childTable;
      const newlyCreatedFkColumns = [];

      for (const pkColumn of pkColumns) {
        const relColumn = rel.columns.find((rc) => rc.refColumnId === pkColumn.id);

        if (relColumn && relColumn.fkColumnId) {
          const existingColumn = childTable.columns.find((c) => c.id === relColumn.fkColumnId);
          if (existingColumn) continue;
        }

        const newColumnId = `fkcol_${parentTable.id}_${pkColumn.id}_${ulid()}`;
        const columnName = `${parentTable.name}_${pkColumn.name}`;

        const newColumn: Column = {
          ...pkColumn,
          isAffected: true,
          id: newColumnId,
          tableId: childTable.id,
          name: columnName,
          ordinalPosition: childTable.columns.length + 1,
          createdAt: new Date(),
          updatedAt: new Date(),
        };

        updatedChildTable = {
          ...updatedChildTable,
          isAffected: true,
          columns: [...updatedChildTable.columns, newColumn],
        };

        newlyCreatedFkColumns.push(newColumn);

        const newRelColumn: RelationshipColumn = {
          id: `rel_col_${parentTable.id}_${pkColumn.id}_${ulid()}`,
          relationshipId: rel.id,
          fkColumnId: newColumnId,
          refColumnId: pkColumn.id,
          seqNo: rel.columns.length + 1,
          isAffected: true,
        };

        const updatedRel: Relationship = {
          ...rel,
          isAffected: true,
          columns: [...rel.columns, newRelColumn],
        };

        const updatedRelHolderTable: Table = {
          ...table,
          isAffected: true,
          relationships: table.relationships.map((r) => (r.id === rel.id ? updatedRel : r)),
        };

        updatedSchema = {
          ...updatedSchema,
          isAffected: true,
          tables: updatedSchema.tables.map((t) => (t.id === updatedRelHolderTable.id ? updatedRelHolderTable : t)),
        };
      }

      updatedSchema = {
        ...updatedSchema,
        isAffected: true,
        tables: updatedSchema.tables.map((t) => (t.id === childTable.id ? updatedChildTable : t)),
      };

      if (rel.kind === 'NON_IDENTIFYING') continue;

      const childPkConstraint = updatedChildTable.constraints.find((c) => c.kind === 'PRIMARY_KEY');

      if (!childPkConstraint || newlyCreatedFkColumns.length <= 0) continue;

      const newPkColumns = [];

      for (const fkColumn of newlyCreatedFkColumns) {
        newPkColumns.push({
          id: `constraint_col_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
          isAffected: true,
          constraintId: childPkConstraint.id,
          columnId: fkColumn.id,
          seqNo: childPkConstraint.columns.length + newPkColumns.length + 1,
        });
      }

      if (newPkColumns.length <= 0) continue;

      const updatedPkConstraint = {
        ...childPkConstraint,
        isAffected: true,
        columns: [...childPkConstraint.columns, ...newPkColumns],
      };

      updatedChildTable = {
        ...updatedChildTable,
        isAffected: true,
        constraints: updatedChildTable.constraints.map((c) =>
          c.id === childPkConstraint.id ? updatedPkConstraint : c
        ),
      };

      updatedSchema = {
        ...updatedSchema,
        isAffected: true,
        tables: updatedSchema.tables.map((t) => (t.id === childTable.id ? updatedChildTable : t)),
      };

      updatedSchema = propagateKeysToChildren(structuredClone(updatedSchema), rel.srcTableId, new Set(visited));
    }
  }

  return updatedSchema;
};

export const deleteRelatedColumns = (
  currentSchema: Schema,
  relationshipToDelete: Relationship,
  visited: Set<string> = new Set()
): Schema => {
  const relationshipKey = relationshipToDelete.id;
  if (visited.has(relationshipKey)) return currentSchema;
  visited.add(relationshipKey);

  let updatedSchema = currentSchema;

  const fkColumnsToDelete = new Set<string>();
  relationshipToDelete.columns.forEach((relCol) => {
    fkColumnsToDelete.add(relCol.fkColumnId);
  });

  updatedSchema = {
    ...updatedSchema,
    isAffected: true,
    tables: updatedSchema.tables.map((table) => {
      if (table.id === relationshipToDelete.srcTableId) {
        const isAffected =
          table.relationships.some((r) => r.id === relationshipToDelete.id) ||
          table.columns.some((col) => fkColumnsToDelete.has(col.id)) ||
          table.indexes.some((idx) => idx.columns.some((ic) => fkColumnsToDelete.has(ic.columnId))) ||
          table.constraints.some((constraint) => constraint.columns.some((cc) => fkColumnsToDelete.has(cc.columnId)));
        return {
          ...table,
          isAffected,
          relationships: table.relationships.filter((r) => r.id !== relationshipToDelete.id),
          columns: table.columns.filter((col) => !fkColumnsToDelete.has(col.id)),
          indexes: table.indexes.map((idx) => ({
            ...idx,
            columns: idx.columns.filter((ic) => !fkColumnsToDelete.has(ic.columnId)),
          })),
          constraints: table.constraints.map((constraint) => ({
            ...constraint,
            columns: constraint.columns.filter((cc) => !fkColumnsToDelete.has(cc.columnId)),
          })),
        };
      }
      return table;
    }),
  };

  if (relationshipToDelete.kind === 'IDENTIFYING') {
    for (const table of updatedSchema.tables) {
      const relationshipsToDelete = table.relationships.filter((rel) => {
        return rel.columns.some((relCol) => fkColumnsToDelete.has(relCol.refColumnId));
      });

      for (const relToDelete of relationshipsToDelete) {
        if (!visited.has(relToDelete.id)) {
          updatedSchema = deleteRelatedColumns(structuredClone(updatedSchema), relToDelete, new Set(visited));
        }
      }
    }
  }

  return updatedSchema;
};
