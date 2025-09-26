import {
  SchemaNotExistError,
  TableNotExistError,
  ColumnNotExistError,
  ColumnNameInvalidError,
  ColumnPositionInvalidError,
} from '../errors';
import { Database, COLUMN, Schema, Table, Column, Constraint } from '../types';
import * as helper from '../helper';

export interface ColumnHandlers {
  createColumn: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    column: Omit<Column, 'tableId' | 'createdAt' | 'updatedAt'>
  ) => Database;
  deleteColumn: (database: Database, schemaId: Schema['id'], tableId: Table['id'], columnId: Column['id']) => Database;
  changeColumnName: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    newName: Column['name']
  ) => Database;
  changeColumnType: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    dataType: Column['dataType'],
    lengthScale?: Column['lengthScale']
  ) => Database;
  changeColumnPosition: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    newPosition: Column['ordinalPosition']
  ) => Database;
  changeColumnNullable: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    nullable: boolean
  ) => Database;
}

export const columnHandlers: ColumnHandlers = {
  createColumn: (database, schemaId, tableId, column) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const result = COLUMN.omit({ id: true, tableId: true, createdAt: true, updatedAt: true }).safeParse(column);
    if (!result.success) throw new ColumnNameInvalidError(column.name);

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
                      columns: [...t.columns, { ...column, tableId, createdAt: new Date(), updatedAt: new Date() }],
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  deleteColumn: (database, schemaId, tableId, columnId) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    // Find all relationships that use this column
    const affectedRelationships = helper.findRelationshipsByColumn(schema, columnId);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) => {
                if (t.id === tableId) {
                  // 현재 테이블에서 컬럼 삭제 및 관련 요소들 정리
                  return {
                    ...t,
                    updatedAt: new Date(),
                    // 컬럼 삭제
                    columns: t.columns.filter((c) => c.id !== columnId),
                    // 인덱스에서 해당 컬럼 제거, 빈 인덱스는 삭제
                    indexes: t.indexes
                      .map((idx) => ({
                        ...idx,
                        columns: idx.columns.filter((ic) => ic.columnId !== columnId),
                      }))
                      .filter((idx) => idx.columns.length > 0),
                    // 제약조건에서 해당 컬럼 제거, 빈 제약조건은 삭제
                    constraints: t.constraints
                      .map((constraint) => ({
                        ...constraint,
                        columns: constraint.columns.filter((cc) => cc.columnId !== columnId),
                      }))
                      .filter((constraint) => constraint.columns.length > 0),
                    // 관계에서 해당 컬럼 제거, 빈 관계는 삭제
                    relationships: t.relationships
                      .map((rel) => ({
                        ...rel,
                        columns: rel.columns.filter((rc) => rc.srcColumnId !== columnId && rc.tgtColumnId !== columnId),
                      }))
                      .filter((rel) => rel.columns.length > 0),
                  };
                } else {
                  // 다른 테이블들에서 해당 컬럼을 참조하는 관계들 정리
                  const hasAffectedRelationships = affectedRelationships.some((ar) => ar.table.id === t.id);

                  if (hasAffectedRelationships) {
                    return {
                      ...t,
                      updatedAt: new Date(),
                      // 관계에서 해당 컬럼 참조 제거 후 빈 관계는 삭제
                      relationships: t.relationships
                        .map((rel) => ({
                          ...rel,
                          columns: rel.columns.filter(
                            (rc) => rc.srcColumnId !== columnId && rc.tgtColumnId !== columnId
                          ),
                        }))
                        .filter((rel) => rel.columns.length > 0),
                    };
                  }

                  return t;
                }
              }),
            }
          : {
              ...s,
              // 다른 스키마의 테이블들에서도 해당 컬럼을 참조하는 관계들 정리
              tables: s.tables.map((t) => {
                const hasAffectedRelationships = affectedRelationships.some((ar) => ar.table.id === t.id);

                if (hasAffectedRelationships) {
                  return {
                    ...t,
                    updatedAt: new Date(),
                    relationships: t.relationships
                      .map((rel) => ({
                        ...rel,
                        columns: rel.columns.filter((rc) => rc.srcColumnId !== columnId && rc.tgtColumnId !== columnId),
                      }))
                      .filter((rel) => rel.columns.length > 0),
                  };
                }

                return t;
              }),
            }
      ),
    };
  },
  changeColumnName: (database, schemaId, tableId, columnId, newName) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    const result = COLUMN.shape.name.safeParse(newName);
    if (!result.success) throw new ColumnNameInvalidError(newName);

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
                      columns: t.columns.map((c) =>
                        c.id === columnId ? { ...c, updatedAt: new Date(), name: newName } : c
                      ),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  changeColumnType: (database, schemaId, tableId, columnId, dataType, lengthScale) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

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
                      columns: t.columns.map((c) =>
                        c.id === columnId
                          ? { ...c, updatedAt: new Date(), dataType, lengthScale: lengthScale || c.lengthScale }
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
  changeColumnPosition: (database, schemaId, tableId, columnId, newPosition) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    const tableColumns = table.columns;

    if (newPosition < 1 || newPosition > tableColumns.length) {
      throw new ColumnPositionInvalidError(newPosition, tableColumns.length);
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
                      columns: t.columns.map((c) =>
                        c.id === columnId ? { ...c, updatedAt: new Date(), ordinalPosition: newPosition } : c
                      ),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  changeColumnNullable: (database, schemaId, tableId, columnId, nullable) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    const currentNullable = helper.isColumnNullable(table, columnId);
    if (currentNullable === nullable) return database;

    // Find relationships that might be affected by this change
    const affectedRelationships = helper.findRelationshipsByColumn(schema, columnId);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) => {
                if (t.id === tableId) {
                  // Update the column's nullable constraint
                  let updatedConstraints = t.constraints;

                  if (nullable) {
                    // Remove NOT_NULL constraint if it exists
                    updatedConstraints = t.constraints.filter(
                      (constraint) =>
                        !(constraint.kind === 'NOT_NULL' && constraint.columns.some((cc) => cc.columnId === columnId))
                    );
                  } else {
                    // Add NOT_NULL constraint if it doesn't exist
                    const hasNotNull = t.constraints.some(
                      (constraint) =>
                        constraint.kind === 'NOT_NULL' && constraint.columns.some((cc) => cc.columnId === columnId)
                    );

                    if (!hasNotNull) {
                      const newConstraint: Constraint = {
                        id: `constraint_${Date.now()}`, // In real implementation, use proper ULID
                        tableId,
                        name: `nn_${column.name}`,
                        kind: 'NOT_NULL',
                        checkExpr: null,
                        defaultExpr: null,
                        columns: [
                          {
                            id: `cc_${Date.now()}`, // In real implementation, use proper ULID
                            constraintId: `constraint_${Date.now()}`,
                            columnId,
                            seqNo: 1,
                          },
                        ],
                      };
                      updatedConstraints = [...t.constraints, newConstraint];
                    }
                  }

                  return {
                    ...t,
                    updatedAt: new Date(),
                    constraints: updatedConstraints,
                    // Update relationships that might be affected
                    relationships: t.relationships.map((rel) => {
                      if (helper.shouldUpdateCardinalityForNullChange(rel, columnId, nullable)) {
                        return {
                          ...rel,
                          cardinality: '1:N' as const, // Change from 1:1 to 1:N when FK becomes nullable
                        };
                      }
                      return rel;
                    }),
                  };
                }

                // Update relationships in other tables that reference this column
                const hasAffectedRelationships = affectedRelationships.some((ar) => ar.table.id === t.id);
                if (hasAffectedRelationships) {
                  return {
                    ...t,
                    updatedAt: new Date(),
                    relationships: t.relationships.map((rel) => {
                      if (helper.shouldUpdateCardinalityForNullChange(rel, columnId, nullable)) {
                        return {
                          ...rel,
                          cardinality: '1:N' as const,
                        };
                      }
                      return rel;
                    }),
                  };
                }

                return t;
              }),
            }
          : s
      ),
    };
  },
};
