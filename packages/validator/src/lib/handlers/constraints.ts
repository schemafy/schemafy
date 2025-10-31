import {
  ConstraintColumnNotExistError,
  ConstraintColumnNotUniqueError,
  ConstraintNameNotUniqueError,
  ConstraintNotExistError,
  ConstraintNullableChangeNotAllowedError,
  DuplicateKeyDefinitionError,
  SchemaNotExistError,
  TableNotExistError,
  UniqueSameAsPrimaryKeyError,
} from '../errors';
import { Column, Constraint, ConstraintColumn, Database, Schema, Table } from '../types';
import * as helper from '../helper';

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
  changeConstraintNullable: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    nullable: boolean
  ) => Database;
}

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
      propagatedSchema = helper.propagateNewPrimaryKey(structuredClone(propagatedSchema), tableId, column);
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

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId
        ? {
            ...t,
            isAffected: t.constraints.some((c) => c.id === constraintId),
            constraints: t.constraints.filter((c) => c.id !== constraintId),
          }
        : t
    );

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s
    );

    let updatedDatabase: Database = { ...database, isAffected: true, schemas: changeSchemas };

    if (constraint.kind !== 'PRIMARY_KEY') return updatedDatabase;

    const columns: Column[] = constraint.columns.map((column) => table.columns.find((c) => c.id === column.columnId)!);

    for (const column of columns) {
      const updatedSchema = updatedDatabase.schemas.find((s) => s.id === schemaId)!;
      const cascadedSchema = helper.deleteCascadingForeignKeys(structuredClone(updatedSchema), tableId, column.id);

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

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId
        ? {
            ...t,
            isAffected: true,
            constraints: t.constraints.map((c) =>
              c.id === constraintId ? { ...c, name: newName, isAffected: true } : c
            ),
          }
        : t
    );

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s
    );

    return { ...database, isAffected: true, schemas: changeSchemas };
  },
  addColumnToConstraint: (database, schemaId, tableId, constraintId, constraintColumn) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId);

    const changeConstraints: Constraint[] = table.constraints.map((c) =>
      c.id === constraintId
        ? {
            ...c,
            columns: [...c.columns, { ...constraintColumn, constraintId, isAffected: true }],
            isAffected: true,
          }
        : c
    );

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId ? { ...t, isAffected: true, constraints: changeConstraints } : t
    );

    let changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s
    );

    if (constraint.kind === 'PRIMARY_KEY') {
      const column = table.columns.find((c) => c.id === constraintColumn.columnId)!;
      let schema = changeSchemas.find((s) => s.id === schemaId)!;
      schema = helper.propagateNewPrimaryKey(structuredClone(schema), tableId, column);
      changeSchemas = database.schemas.map((s) => (s.id === schemaId ? { ...schema, isAffected: true } : s));
    }

    return { ...database, isAffected: true, schemas: changeSchemas };
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

    const changeConstraints: Constraint[] = table.constraints.map((c) =>
      c.id === constraintId
        ? {
            ...c,
            isAffected: c.columns.some((cc) => cc.id === constraintColumnId),
            columns: c.columns.filter((cc) => cc.id !== constraintColumnId),
          }
        : c
    );

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId ? { ...t, isAffected: true, constraints: changeConstraints } : t
    );

    let changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s
    );

    if (constraint.kind === 'PRIMARY_KEY') {
      const column = table.columns.find((c) => c.id === constraintColumn.columnId)!;
      let schema = changeSchemas.find((s) => s.id === schemaId)!;
      schema = helper.propagateNewPrimaryKey(structuredClone(schema), tableId, column);
      changeSchemas = database.schemas.map((s) => (s.id === schemaId ? { ...schema, isAffected: true } : s));
    }

    return {
      ...database,
      isAffected: true,
      schemas: changeSchemas,
    };
  },
  changeConstraintNullable: (database, schemaId, tableId, constraintId, nullable) => {
    // NOTE: 변경에 의해서도 이전과 이후가 같은 상황에 대해선 고려가 필요없는지?

    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId);

    if (constraint.kind === 'PRIMARY_KEY' || constraint.kind === 'UNIQUE')
      throw new ConstraintNullableChangeNotAllowedError(constraintId);

    let currentDatabase: Database = constraintHandlers.deleteConstraint(
      structuredClone(database),
      schemaId,
      tableId,
      constraintId
    );

    const updatedConstraint: Constraint = {
      ...constraint,
      isAffected: true,
      kind: nullable ? 'NOT_NULL' : 'DEFAULT',
    };

    currentDatabase = constraintHandlers.createConstraint(
      structuredClone(currentDatabase),
      schemaId,
      tableId,
      updatedConstraint
    );

    // if (!nullable) {
    //   const updatedSchema = currentDatabase.schemas.find((s) => s.id === schemaId)!;
    //   const updatedTable = updatedSchema.tables.find((t) => t.id === tableId)!;

    //   const hasNotNull = updatedTable.constraints.some(
    //     (constraint) => constraint.kind === 'NOT_NULL' && constraint.columns.some((cc) => cc.columnId === columnId)
    //   );

    //   if (!hasNotNull) {
    //     const newConstraintId = `constraint_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    //     const newConstraint: Constraint = {
    //       id: newConstraintId,
    //       tableId,
    //       name: `nn_${column.name}`,
    //       kind: 'NOT_NULL' as const,
    //       checkExpr: null,
    //       defaultExpr: null,
    //       columns: [
    //         {
    //           id: `cc_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    //           constraintId: newConstraintId,
    //           columnId,
    //           seqNo: 1,
    //           isAffected: true,
    //         },
    //       ],
    //       isAffected: true,
    //     };

    //     currentDatabase = {
    //       ...currentDatabase,
    //       isAffected: true,
    //       schemas: currentDatabase.schemas.map((s) =>
    //         s.id === schemaId
    //           ? {
    //               ...s,
    //               isAffected: true,
    //               tables: s.tables.map((t) =>
    //                 t.id === tableId
    //                   ? {
    //                       ...t,
    //                       isAffected: true,
    //                       constraints: [...t.constraints, newConstraint],
    //                     }
    //                   : t
    //               ),
    //             }
    //           : s
    //       ),
    //     };
    //   }
    // }

    return currentDatabase;
  },
};
