import { ulid } from 'ulid';
import {
  ConstraintColumnNotExistError,
  ConstraintColumnNotUniqueError,
  ConstraintNameNotUniqueError,
  ConstraintNotExistError,
  DuplicateKeyDefinitionError,
  SchemaNotExistError,
  TableNotExistError,
  UniqueSameAsPrimaryKeyError,
} from '../errors';
import {
  Column,
  Constraint,
  ConstraintColumn,
  Database,
  Index,
  Relationship,
  RelationshipColumn,
  Schema,
  Table,
} from '../types';

export interface ConstraintHandlers {
  createConstraint: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraint: Omit<Constraint, 'tableId'>
  ) => Database;
  deleteConstraint: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id']
  ) => Database;
  changeConstraintName: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    newName: Constraint['name']
  ) => Database;
  addColumnToConstraint: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    constraintColumn: Omit<ConstraintColumn, 'constraintId'>
  ) => Database;
  removeColumnFromConstraint: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    constraintColumnId: ConstraintColumn['id']
  ) => Database;
}

const propagateNewPrimaryKey = (
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

const deleteCascadingForeignKeys = (
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

export const constraintHandlers: ConstraintHandlers = {
  createConstraint: (database, schemaId, tableId, constraint) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    for (const schemaTable of schema.tables) {
      for (const c of schemaTable.constraints) {
        if (c.name === constraint.name) throw new ConstraintNameNotUniqueError(constraint.name, schemaId);
      }
    }

    const constraintColumnIds = new Set<string>();
    for (const constraintColumn of constraint.columns) {
      const columnExists = table.columns.some((col) => col.id === constraintColumn.columnId);
      if (!columnExists) {
        throw new ConstraintColumnNotExistError(constraintColumn.columnId, constraint.name);
      }

      if (constraintColumnIds.has(constraintColumn.columnId)) {
        throw new ConstraintColumnNotUniqueError(constraintColumn.columnId, constraint.name);
      }
      constraintColumnIds.add(constraintColumn.columnId);
    }

    const existingConstraint = table.constraints.find((c) => {
      if (
        c.kind !== constraint.kind ||
        c.checkExpr !== constraint.checkExpr ||
        c.defaultExpr !== constraint.defaultExpr
      )
        return false;

      const existingColumnIds = c.columns.map((col) => col.columnId).sort();
      const newColumnIds = constraint.columns.map((col) => col.columnId).sort();

      return JSON.stringify(existingColumnIds) === JSON.stringify(newColumnIds);
    });

    if (existingConstraint) {
      throw new DuplicateKeyDefinitionError(constraint.name, existingConstraint.name);
    }

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId
        ? { ...t, isAffected: true, constraints: [...t.constraints, { ...constraint, isAffected: true, tableId }] }
        : { ...t, isAffected: true }
    );

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s
    );

    let updatedDatabase: Database = {
      ...database,
      isAffected: true,
      schemas: changeSchemas,
    };

    if (constraint.kind === 'UNIQUE') {
      const pkConstraint = table.constraints.find((c) => c.kind === 'PRIMARY_KEY');
      if (pkConstraint) {
        const pkColumnIds = pkConstraint.columns.map((col) => col.columnId).sort();
        const uniqueColumnIds = constraint.columns.map((col) => col.columnId).sort();

        if (JSON.stringify(pkColumnIds) === JSON.stringify(uniqueColumnIds)) {
          throw new UniqueSameAsPrimaryKeyError(constraint.name);
        }
      }
    }

    if (constraint.kind !== 'PRIMARY_KEY') return updatedDatabase;

    const columns = constraint.columns.map((column) => table.columns.find((c) => c.id === column.columnId)!);

    let propagatedSchema: Schema = updatedDatabase.schemas.find((s) => s.id === schemaId)!;

    columns.forEach((column) => {
      propagatedSchema = propagateNewPrimaryKey(structuredClone(propagatedSchema), tableId, column);
    });

    updatedDatabase = {
      ...updatedDatabase,
      isAffected: true,
      schemas: updatedDatabase.schemas.map((s) => (s.id === schemaId ? { ...propagatedSchema, isAffected: true } : s)),
    };

    return updatedDatabase;
  },
  deleteConstraint: (database, schemaId, tableId, constraintId) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId);

    let updatedDatabase: Database = {
      ...database,
      isAffected: true,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              isAffected: true,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      isAffected: t.constraints.some((c) => c.id === constraintId),
                      constraints: t.constraints.filter((c) => c.id !== constraintId),
                    }
                  : t
              ),
            }
          : s
      ),
    };

    if (constraint.kind !== 'PRIMARY_KEY') return updatedDatabase;

    const columns: Column[] = constraint.columns.map((column) => table.columns.find((c) => c.id === column.columnId)!);

    for (const column of columns) {
      const updatedSchema = updatedDatabase.schemas.find((s) => s.id === schemaId)!;
      const cascadedSchema = deleteCascadingForeignKeys(structuredClone(updatedSchema), tableId, column.id);

      updatedDatabase = {
        ...updatedDatabase,
        isAffected: true,
        schemas: updatedDatabase.schemas.map((s) => (s.id === schemaId ? { ...cascadedSchema, isAffected: true } : s)),
      };
    }

    return updatedDatabase;
  },
  changeConstraintName: (database, schemaId, tableId, constraintId, newName) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId);

    return {
      ...database,
      isAffected: true,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              isAffected: true,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      isAffected: true,
                      constraints: t.constraints.map((c) =>
                        c.id === constraintId ? { ...c, name: newName, isAffected: true } : c
                      ),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  addColumnToConstraint: (database, schemaId, tableId, constraintId, constraintColumn) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId);

    return {
      ...database,
      isAffected: true,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              isAffected: true,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      isAffected: true,
                      constraints: t.constraints.map((c) =>
                        c.id === constraintId
                          ? {
                              ...c,
                              isAffected: true,
                              columns: [
                                ...c.columns,
                                {
                                  ...constraintColumn,
                                  constraintId,
                                  isAffected: true,
                                },
                              ],
                            }
                          : c
                      ),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  removeColumnFromConstraint: (database, schemaId, tableId, constraintId, constraintColumnId) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId);

    const constraintColumn = constraint.columns.find((cc) => cc.id === constraintColumnId);
    if (!constraintColumn) throw new ConstraintColumnNotExistError(constraintColumnId);

    return {
      ...database,
      isAffected: true,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              isAffected: true,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      isAffected: true,
                      constraints: t.constraints
                        .map((c) =>
                          c.id === constraintId
                            ? {
                                ...c,
                                isAffected: c.columns.some((cc) => cc.id === constraintColumnId),
                                columns: c.columns.filter((cc) => cc.id !== constraintColumnId),
                              }
                            : c
                        )
                        .filter((c) => c.columns.length > 0),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
};
