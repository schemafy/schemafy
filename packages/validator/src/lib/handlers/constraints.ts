import {
  SchemaNotExistError,
  TableNotExistError,
  ConstraintNotExistError,
  ConstraintNameInvalidError,
  ConstraintColumnNotExistError,
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
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    // Validate constraint name directly since CONSTRAINT has refinement
    if (!constraint.name || typeof constraint.name !== 'string') {
      throw new ConstraintNameInvalidError(constraint.name);
    }

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
                      constraints: [...t.constraints, { ...constraint, tableId }],
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  deleteConstraint: (database, schemaId, tableId, constraintId) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
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
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId);

    // Validate constraint name directly since CONSTRAINT has refinement
    if (!newName || typeof newName !== 'string') {
      throw new ConstraintNameInvalidError(newName);
    }

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
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
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
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
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId);

    const constraintColumn = constraint.columns.find((cc) => cc.id === constraintColumnId);
    if (!constraintColumn) throw new ConstraintColumnNotExistError(constraintColumnId);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
                      // 제약조건에서 컬럼 제거 후, 빈 제약조건은 삭제
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
