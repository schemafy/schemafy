import {
  SchemaNotExistError,
  TableNotExistError,
  ConstraintNotExistError,
  ConstraintColumnNotExistError,
  DuplicateKeyDefinitionError,
  ConstraintNameNotUniqueError,
  ConstraintColumnNotExistError as ConstraintColumnNotExistValidationError,
  ConstraintColumnNotUniqueError,
  UniqueSameAsPrimaryKeyError,
} from '../errors';
import { Database, Schema, Table, Constraint, ConstraintColumn } from '../types';

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

export const constraintHandlers: ConstraintHandlers = {
  createConstraint: (database, schemaId, tableId, constraint) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const newDatabase = {
      ...database,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      constraints: [...t.constraints, { ...constraint, tableId }],
                    }
                  : t
              ),
            }
          : s
      ),
    };

    const constraintNames = new Set<string>();
    for (const schemaTable of schema.tables) {
      for (const c of schemaTable.constraints) {
        const fullConstraintName = `${schema.name}.${c.name}`;
        constraintNames.add(fullConstraintName);
      }
    }

    const newFullConstraintName = `${schema.name}.${constraint.name}`;
    if (constraintNames.has(newFullConstraintName)) {
      throw new ConstraintNameNotUniqueError(constraint.name, schemaId);
    }

    const constraintColumnIds = new Set<string>();
    for (const constraintColumn of constraint.columns) {
      const columnExists = table.columns.some((col) => col.id === constraintColumn.columnId);
      if (!columnExists) {
        throw new ConstraintColumnNotExistValidationError(constraintColumn.columnId, constraint.name);
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

    let updatedDatabase = newDatabase;
    if (constraint.kind === 'PRIMARY_KEY') {
      for (const constraintColumn of constraint.columns) {
        const hasNotNull = table.constraints.some(
          (c) => c.kind === 'NOT_NULL' && c.columns.some((cc) => cc.columnId === constraintColumn.columnId)
        );

        if (!hasNotNull) {
          const constraintId = `nn_${constraintColumn.columnId}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
          const notNullConstraint = {
            id: constraintId,
            tableId: tableId,
            name: `nn_${table.name}_${constraintColumn.columnId}`,
            kind: 'NOT_NULL' as const,
            columns: [
              {
                id: `nncol_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
                columnId: constraintColumn.columnId,
                seqNo: 1,
                constraintId: constraintId,
              },
            ],
            checkExpr: undefined,
            defaultExpr: undefined,
          };

          updatedDatabase = {
            ...updatedDatabase,
            schemas: updatedDatabase.schemas.map((s) =>
              s.id === schemaId
                ? {
                    ...s,
                    tables: s.tables.map((t) =>
                      t.id === tableId
                        ? {
                            ...t,
                            constraints: [...t.constraints, notNullConstraint],
                          }
                        : t
                    ),
                  }
                : s
            ),
          };
        }
      }
    }

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

    return updatedDatabase;
  },
  deleteConstraint: (database, schemaId, tableId, constraintId) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId);

    return {
      ...database,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      constraints: t.constraints.filter((c) => c.id !== constraintId),
                    }
                  : t
              ),
            }
          : s
      ),
    };
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
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      constraints: t.constraints.map((c) => (c.id === constraintId ? { ...c, name: newName } : c)),
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
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      constraints: t.constraints.map((c) =>
                        c.id === constraintId
                          ? {
                              ...c,
                              columns: [...c.columns, { ...constraintColumn, constraintId }],
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
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      constraints: t.constraints
                        .map((c) =>
                          c.id === constraintId
                            ? {
                                ...c,
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
