import {
  SchemaNotExistError,
  TableNotExistError,
  ConstraintNotExistError,
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
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

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
