import {
  ConstraintColumnNotExistError,
  ConstraintColumnNotUniqueError,
  ConstraintNameNotUniqueError,
  ConstraintNotExistError,
  DuplicateKeyDefinitionError,
  SchemaNotExistError,
  TableNotExistError,
  UniqueSameAsPrimaryKeyError,
} from "../errors";
import type {
  Column,
  Constraint,
  ConstraintColumn,
  Database,
  Schema,
  Table,
} from "@/types/erd.types";
import * as helper from "../helper";

export interface ConstraintHandlers {
  createConstraint: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    constraint: Omit<Constraint, "tableId">,
  ) => Database;
  deleteConstraint: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    constraintId: Constraint["id"],
  ) => Database;
  changeConstraintName: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    constraintId: Constraint["id"],
    newName: Constraint["name"],
  ) => Database;
  addColumnToConstraint: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    constraintId: Constraint["id"],
    constraintColumn: Omit<ConstraintColumn, "constraintId">,
  ) => Database;
  removeColumnFromConstraint: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    constraintId: Constraint["id"],
    constraintColumnId: ConstraintColumn["id"],
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
        if (c.name === constraint.name)
          throw new ConstraintNameNotUniqueError(constraint.name, schemaId);
      }
    }

    const constraintColumnIds = new Set<string>();
    for (const constraintColumn of constraint.columns) {
      const columnExists = table.columns.some(
        (col) => col.id === constraintColumn.columnId,
      );
      if (!columnExists) {
        throw new ConstraintColumnNotExistError(
          constraintColumn.columnId,
          constraint.name,
        );
      }

      if (constraintColumnIds.has(constraintColumn.columnId)) {
        throw new ConstraintColumnNotUniqueError(
          constraintColumn.id,
          constraint.name,
        );
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
      throw new DuplicateKeyDefinitionError(
        constraint.name,
        existingConstraint.name,
      );
    }

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId
        ? {
            ...t,
            isAffected: true,
            constraints: [
              ...t.constraints,
              {
                ...constraint,
                tableId,
                isAffected: true,
                columns: constraint.columns.map((column) => ({
                  ...column,
                  constraintId: constraint.id,
                  isAffected: true,
                })),
              },
            ],
          }
        : { ...t, isAffected: true },
    );

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s,
    );

    let updatedDatabase: Database = {
      ...database,
      isAffected: true,
      schemas: changeSchemas,
    };

    if (constraint.kind === "UNIQUE") {
      const pkConstraint = table.constraints.find(
        (c) => c.kind === "PRIMARY_KEY",
      );
      if (pkConstraint) {
        const pkColumnIds = pkConstraint.columns
          .map((col) => col.columnId)
          .sort();
        const uniqueColumnIds = constraint.columns
          .map((col) => col.columnId)
          .sort();

        if (JSON.stringify(pkColumnIds) === JSON.stringify(uniqueColumnIds)) {
          throw new UniqueSameAsPrimaryKeyError(
            constraint.name,
            pkConstraint.name,
          );
        }
      }
    }

    if (constraint.kind !== "PRIMARY_KEY") return updatedDatabase;

    const columns = constraint.columns.map(
      (column) => table.columns.find((c) => c.id === column.columnId)!,
    );

    let propagatedSchema: Schema = updatedDatabase.schemas.find(
      (s) => s.id === schemaId,
    )!;

    columns.forEach((column) => {
      propagatedSchema = helper.propagateNewPrimaryKey(
        structuredClone(propagatedSchema),
        tableId,
        column,
      );
    });

    updatedDatabase = {
      ...updatedDatabase,
      isAffected: true,
      schemas: updatedDatabase.schemas.map((s) =>
        s.id === schemaId ? { ...propagatedSchema, isAffected: true } : s,
      ),
    };

    return updatedDatabase;
  },
  deleteConstraint: (database, schemaId, tableId, constraintId) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId, tableId);

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId
        ? {
            ...t,
            isAffected: t.constraints.some((c) => c.id === constraintId),
            constraints: t.constraints.filter((c) => c.id !== constraintId),
          }
        : t,
    );

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s,
    );

    let updatedDatabase: Database = {
      ...database,
      isAffected: true,
      schemas: changeSchemas,
    };

    if (constraint.kind !== "PRIMARY_KEY") return updatedDatabase;

    const columns: Column[] = constraint.columns.map(
      (column) => table.columns.find((c) => c.id === column.columnId)!,
    );

    for (const column of columns) {
      const updatedSchema = updatedDatabase.schemas.find(
        (s) => s.id === schemaId,
      )!;
      const cascadedSchema = helper.deleteCascadingForeignKeys(
        structuredClone(updatedSchema),
        tableId,
        column.id,
      );

      updatedDatabase = {
        ...updatedDatabase,
        isAffected: true,
        schemas: updatedDatabase.schemas.map((s) =>
          s.id === schemaId ? { ...cascadedSchema, isAffected: true } : s,
        ),
      };
    }

    return updatedDatabase;
  },
  changeConstraintName: (
    database,
    schemaId,
    tableId,
    constraintId,
    newName,
  ) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId, tableId);

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId
        ? {
            ...t,
            isAffected: true,
            constraints: t.constraints.map((c) =>
              c.id === constraintId
                ? { ...c, name: newName, isAffected: true }
                : c,
            ),
          }
        : t,
    );

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s,
    );

    return { ...database, isAffected: true, schemas: changeSchemas };
  },
  addColumnToConstraint: (
    database,
    schemaId,
    tableId,
    constraintId,
    constraintColumn,
  ) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId, tableId);

    const changeConstraints: Constraint[] = table.constraints.map((c) =>
      c.id === constraintId
        ? {
            ...c,
            columns: [
              ...c.columns,
              { ...constraintColumn, constraintId, isAffected: true },
            ],
            isAffected: true,
          }
        : c,
    );

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId
        ? { ...t, isAffected: true, constraints: changeConstraints }
        : t,
    );

    let changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s,
    );

    if (constraint.kind === "PRIMARY_KEY") {
      const column = table.columns.find(
        (c) => c.id === constraintColumn.columnId,
      )!;
      let schema = changeSchemas.find((s) => s.id === schemaId)!;
      schema = helper.propagateNewPrimaryKey(
        structuredClone(schema),
        tableId,
        column,
      );
      changeSchemas = database.schemas.map((s) =>
        s.id === schemaId ? { ...schema, isAffected: true } : s,
      );
    }

    return { ...database, isAffected: true, schemas: changeSchemas };
  },
  removeColumnFromConstraint: (
    database,
    schemaId,
    tableId,
    constraintId,
    constraintColumnId,
  ) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) throw new ConstraintNotExistError(constraintId, tableId);

    const constraintColumn = constraint.columns.find(
      (cc) => cc.id === constraintColumnId,
    );
    if (!constraintColumn)
      throw new ConstraintColumnNotExistError(
        constraintColumnId,
        constraint.name,
      );

    const changeConstraints: Constraint[] = table.constraints.map((c) =>
      c.id === constraintId
        ? {
            ...c,
            isAffected: c.columns.some((cc) => cc.id === constraintColumnId),
            columns: c.columns.filter((cc) => cc.id !== constraintColumnId),
          }
        : c,
    );

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId
        ? { ...t, isAffected: true, constraints: changeConstraints }
        : t,
    );

    let changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s,
    );

    if (constraint.kind === "PRIMARY_KEY") {
      let schema = changeSchemas.find((s) => s.id === schemaId)!;
      schema = helper.deleteCascadingForeignKeys(
        structuredClone(schema),
        tableId,
        constraintColumn.columnId,
      );
      changeSchemas = database.schemas.map((s) =>
        s.id === schemaId ? { ...schema, isAffected: true } : s,
      );
    }

    return {
      ...database,
      isAffected: true,
      schemas: changeSchemas,
    };
  },
};
