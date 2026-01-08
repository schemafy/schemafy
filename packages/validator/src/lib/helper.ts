import { ulid } from "ulid";
import type {
  Column,
  Constraint,
  Index,
  Relationship,
  RelationshipColumn,
  Schema,
  Table,
} from "..";

export const detectIdentifyingCycleInSchema = (
  schema: Schema,
  pendingRelationshipChange?: { relationshipId: string; newKind: string },
  newRelationship?: { fkTableId: string; pkTableId: string; kind: string },
): [Table["id"], Table["id"]] | null => {
  const graph = new Map<string, string[]>();

  for (const table of schema.tables) {
    for (const rel of table.relationships) {
      let effectiveKind = rel.kind;
      if (
        pendingRelationshipChange &&
        rel.id === pendingRelationshipChange.relationshipId
      ) {
        effectiveKind = pendingRelationshipChange.newKind as typeof rel.kind;
      }

      if (effectiveKind !== "IDENTIFYING") continue;

      if (!graph.has(rel.fkTableId)) {
        graph.set(rel.fkTableId, []);
      }
      graph.get(rel.fkTableId)!.push(rel.pkTableId);
    }
  }

  if (newRelationship && newRelationship.kind === "IDENTIFYING") {
    if (!graph.has(newRelationship.fkTableId)) {
      graph.set(newRelationship.fkTableId, []);
    }
    graph.get(newRelationship.fkTableId)!.push(newRelationship.pkTableId);
  }

  const visited = new Set<string>();
  const recursionStack = new Set<string>();

  const dfs = (tableId: string, path: string[]): [string, string] | null => {
    if (recursionStack.has(tableId)) {
      return [path[path.length - 1], tableId];
    }
    if (visited.has(tableId)) return null;

    visited.add(tableId);
    recursionStack.add(tableId);

    const neighbors = graph.get(tableId) ?? [];
    for (const neighbor of neighbors) {
      const cycle = dfs(neighbor, [...path, tableId]);
      if (cycle) return cycle;
    }

    recursionStack.delete(tableId);
    return null;
  };

  for (const tableId of graph.keys()) {
    if (!visited.has(tableId)) {
      const cycle = dfs(tableId, []);
      if (cycle) return cycle;
    }
  }

  return null;
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

  if (/[^a-zA-Z0-9_]/.test(str)) {
    return false;
  }

  return true;
};

export const precisionRequired = ["DECIMAL", "NUMERIC"];
export const lengthScaleRequired = ["VARCHAR", "CHAR"];

export const categorizedMysqlDataTypes = [
  "TINYINT",
  "SMALLINT",
  "MEDIUMINT",
  "INT",
  "INTEGER",
  "BIGINT",
  "FLOAT",
  "DOUBLE",
  "REAL",
  "DECIMAL",
  "NUMERIC",
  "BIT",
  "BOOL",
  "BOOLEAN",
  "CHAR",
  "VARCHAR",
  "TINYTEXT",
  "TEXT",
  "MEDIUMTEXT",
  "LONGTEXT",
  "BINARY",
  "VARBINARY",
  "BLOB",
  "TINYBLOB",
  "MEDIUMBLOB",
  "LONGBLOB",
  "ENUM",
  "SET",
  "DATE",
  "TIME",
  "DATETIME",
  "TIMESTAMP",
  "YEAR",
  "GEOMETRY",
  "POINT",
  "LINESTRING",
  "POLYGON",
  "MULTIPOINT",
  "MULTILINESTRING",
  "MULTIPOLYGON",
  "GEOMETRYCOLLECTION",
  "JSON",
];

export const propagateNewPrimaryKey = (
  currentSchema: Schema,
  parentTableId: Table["id"],
  newPkColumn: Column,
  visited: Set<string> = new Set(),
): Schema => {
  if (visited.has(parentTableId)) return currentSchema;
  visited.add(parentTableId);

  let updatedSchema = currentSchema;

  for (const table of updatedSchema.tables) {
    for (const rel of table.relationships) {
      if (rel.pkTableId !== parentTableId) continue;

      const childTable = updatedSchema.tables.find(
        (t) => t.id === rel.fkTableId,
      );
      if (!childTable) continue;

      const parentTable = updatedSchema.tables.find(
        (t) => t.id === parentTableId,
      )!;

      const relColumn = rel.columns.find(
        (rc) => rc.pkColumnId === newPkColumn.id,
      );

      if (relColumn && relColumn.fkColumnId && relColumn.fkColumnId !== "") {
        const existingColumn = childTable.columns.find(
          (c) => c.id === relColumn.fkColumnId,
        );
        if (existingColumn) continue;
      }

      const newFkColumnId =
        relColumn?.fkColumnId && relColumn.fkColumnId !== ""
          ? relColumn.fkColumnId
          : `${ulid()}`;
      const columnName = `${parentTable.name}_${newPkColumn.name}`;

      const newFkColumn: Column = {
        ...newPkColumn,
        isAffected: true,
        id: newFkColumnId,
        tableId: childTable.id,
        name: columnName,
        seqNo: childTable.columns.length,
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
            : t,
        ),
      };

      if (rel.kind === "IDENTIFYING") {
        const childPkConstraint = updatedSchema.tables
          .find((t) => t.id === childTable.id)
          ?.constraints.find((c) => c.kind === "PRIMARY_KEY");

        if (childPkConstraint) {
          const newConstraintColumn = {
            id: `${ulid()}`,
            isAffected: true,
            columnId: newFkColumnId,
            seqNo: childPkConstraint.columns.length,
            constraintId: childPkConstraint.id,
          };

          const updatedPkConstraint = {
            ...childPkConstraint,
            isAffected: true,
            columns: [...childPkConstraint.columns, newConstraintColumn],
          };

          updatedSchema = {
            ...updatedSchema,
            isAffected: true,
            tables: updatedSchema.tables.map((t) =>
              t.id === childTable.id
                ? {
                    ...t,
                    isAffected: true,
                    constraints: t.constraints.map((c) =>
                      c.id === childPkConstraint.id ? updatedPkConstraint : c,
                    ),
                  }
                : t,
            ),
          };
        } else {
          const newPkConstraintId = `${ulid()}`;

          const childPkConstraint: Constraint = {
            id: newPkConstraintId,
            name: `constraint_${parentTable.name}_${newPkColumn.name}`,
            columns: [
              {
                id: `${ulid()}`,
                isAffected: true,
                columnId: newFkColumnId,
                seqNo: 0,
                constraintId: newPkConstraintId,
              },
            ],
            tableId: childTable.id,
            kind: "PRIMARY_KEY",
            isAffected: true,
          };

          updatedSchema = {
            ...updatedSchema,
            isAffected: true,
            tables: updatedSchema.tables.map((t) =>
              t.id === childTable.id
                ? {
                    ...t,
                    isAffected: true,
                    constraints: [...t.constraints, childPkConstraint],
                  }
                : t,
            ),
          };
        }
      }

      if (!relColumn) {
        const newRelColumn: RelationshipColumn = {
          id: `${ulid()}`,
          relationshipId: rel.id,
          fkColumnId: newFkColumnId,
          pkColumnId: newPkColumn.id,
          seqNo: rel.columns.length,
          isAffected: true,
        };

        const updateRelationships: Relationship[] = table.relationships.map(
          (r) =>
            r.id === rel.id
              ? {
                  ...r,
                  isAffected: true,
                  columns: [...r.columns, newRelColumn],
                }
              : r,
        );

        const updateTables: Table[] = updatedSchema.tables.map((t) =>
          t.id === table.id
            ? {
                ...t,
                isAffected: true,
                relationships: updateRelationships,
              }
            : t,
        );

        updatedSchema = {
          ...updatedSchema,
          isAffected: true,
          tables: updateTables,
        };
      }

      if (rel.kind === "IDENTIFYING") {
        updatedSchema = propagateNewPrimaryKey(
          structuredClone(updatedSchema),
          rel.fkTableId,
          newFkColumn,
          new Set(visited),
        );
      }
    }
  }

  return updatedSchema;
};

export const deleteCascadingForeignKeys = (
  currentSchema: Schema,
  parentTableId: Table["id"],
  deletedPkColumnId: Column["id"],
  visited: Set<string> = new Set(),
): Schema => {
  if (visited.has(parentTableId)) return currentSchema;
  visited.add(parentTableId);

  let updatedSchema = currentSchema;

  for (const table of updatedSchema.tables) {
    for (const rel of table.relationships) {
      if (rel.pkTableId !== parentTableId) continue;

      const childTable = updatedSchema.tables.find(
        (t) => t.id === rel.fkTableId,
      );

      if (!childTable) continue;

      const fkColumnsToDelete = rel.columns
        .filter((relCol) => relCol.pkColumnId === deletedPkColumnId)
        .map((relCol) => relCol.fkColumnId);

      if (fkColumnsToDelete.length === 0) continue;

      const updateIndexes: Index[] = childTable.indexes
        .map((idx) => ({
          ...idx,
          isAffected: idx.columns.some((ic) =>
            fkColumnsToDelete.includes(ic.columnId),
          ),
          columns: idx.columns.filter(
            (ic) => !fkColumnsToDelete.includes(ic.columnId),
          ),
        }))
        .filter((idx) => idx.columns.length > 0);

      const updateConstraints: Constraint[] = childTable.constraints
        .map((constraint) => {
          const remainingColumns = constraint.columns.filter(
            (cc) => !fkColumnsToDelete.includes(cc.columnId),
          );

          const finalColumns =
            constraint.kind === "PRIMARY_KEY"
              ? remainingColumns.map((cc, index) => ({
                  ...cc,
                  seqNo: index,
                  isAffected: true,
                }))
              : remainingColumns;

          return {
            ...constraint,
            isAffected: constraint.columns.some((cc) =>
              fkColumnsToDelete.includes(cc.columnId),
            ),
            columns: finalColumns,
          };
        })
        .filter((constraint) => constraint.columns.length > 0);

      const updateRelationships: Relationship[] = childTable.relationships
        .map((relationship) => ({
          ...relationship,
          isAffected: relationship.columns.some(
            (rc) =>
              fkColumnsToDelete.includes(rc.fkColumnId) ||
              fkColumnsToDelete.includes(rc.pkColumnId),
          ),
          columns: relationship.columns.filter(
            (rc) =>
              !fkColumnsToDelete.includes(rc.fkColumnId) &&
              !fkColumnsToDelete.includes(rc.pkColumnId),
          ),
        }))
        .filter((relationship) => relationship.columns.length > 0);

      const updateTables: Table[] = updatedSchema.tables.map((t) =>
        t.id === childTable.id
          ? {
              ...t,
              isAffected: true,
              columns: t.columns.filter(
                (col) => !fkColumnsToDelete.includes(col.id),
              ),
              indexes: updateIndexes,
              constraints: updateConstraints,
              relationships: updateRelationships,
            }
          : t,
      );

      updatedSchema = {
        ...updatedSchema,
        isAffected: true,
        tables: updateTables,
      };

      if (rel.kind === "IDENTIFYING") {
        for (const fkColumnId of fkColumnsToDelete) {
          updatedSchema = deleteCascadingForeignKeys(
            structuredClone(updatedSchema),
            rel.fkTableId,
            fkColumnId,
            new Set(visited),
          );
        }
      }
    }
  }

  return updatedSchema;
};

export const removeCascadingPrimaryKeyConstraints = (
  currentSchema: Schema,
  parentTableId: Table["id"],
  removedPkColumnId: Column["id"],
  visited: Set<string> = new Set(),
): Schema => {
  if (visited.has(parentTableId)) return currentSchema;
  visited.add(parentTableId);

  let updatedSchema = currentSchema;

  for (const table of updatedSchema.tables) {
    for (const rel of table.relationships) {
      if (rel.pkTableId !== parentTableId || rel.kind !== "IDENTIFYING")
        continue;

      const childTable = updatedSchema.tables.find(
        (t) => t.id === rel.fkTableId,
      );

      if (!childTable) continue;

      const fkColumnsToRemoveFromPk = rel.columns
        .filter((relCol) => relCol.pkColumnId === removedPkColumnId)
        .map((relCol) => relCol.fkColumnId);

      if (fkColumnsToRemoveFromPk.length === 0) continue;

      const pkConstraint = childTable.constraints.find(
        (c) => c.kind === "PRIMARY_KEY",
      );

      if (!pkConstraint) continue;

      const remainingColumns = pkConstraint.columns.filter(
        (cc) => !fkColumnsToRemoveFromPk.includes(cc.columnId),
      );

      let updateConstraints: Constraint[] = childTable.constraints;

      if (remainingColumns.length !== pkConstraint.columns.length) {
        if (remainingColumns.length === 0) {
          updateConstraints = childTable.constraints.filter(
            (c) => c.id !== pkConstraint.id,
          );
        } else {
          const resequencedColumns = remainingColumns.map((cc, index) => ({
            ...cc,
            seqNo: index,
            isAffected: true,
          }));

          const updatedPkConstraint: Constraint = {
            ...pkConstraint,
            isAffected: true,
            columns: resequencedColumns,
          };

          updateConstraints = childTable.constraints.map((c) =>
            c.id === pkConstraint.id ? updatedPkConstraint : c,
          );
        }
      }

      const updateTables: Table[] = updatedSchema.tables.map((t) =>
        t.id === childTable.id
          ? {
              ...t,
              isAffected: true,
              constraints: updateConstraints,
            }
          : t,
      );

      updatedSchema = {
        ...updatedSchema,
        isAffected: true,
        tables: updateTables,
      };

      for (const fkColumnId of fkColumnsToRemoveFromPk) {
        updatedSchema = removeCascadingPrimaryKeyConstraints(
          structuredClone(updatedSchema),
          rel.fkTableId,
          fkColumnId,
          new Set(visited),
        );
      }
    }
  }

  return updatedSchema;
};

export const propagateKeysToChildren = (
  currentSchema: Schema,
  parentTableId: Table["id"],
  visited: Set<Table["id"]> = new Set(),
): Schema => {
  if (visited.has(parentTableId)) return currentSchema;
  visited.add(parentTableId);

  const parentTable = currentSchema.tables.find((t) => t.id === parentTableId);
  if (!parentTable) return currentSchema;

  const pkConstraint = parentTable.constraints.find(
    (c) => c.kind === "PRIMARY_KEY",
  );
  if (!pkConstraint) return currentSchema;

  const pkColumnIds = pkConstraint.columns.map((cc) => cc.columnId);
  const pkColumns = parentTable.columns.filter((col) =>
    pkColumnIds.includes(col.id),
  );

  let updatedSchema = currentSchema;

  for (const table of updatedSchema.tables) {
    for (const rel of table.relationships) {
      if (rel.pkTableId !== parentTableId) continue;

      const childTable = updatedSchema.tables.find(
        (t) => t.id === rel.fkTableId,
      );
      if (!childTable) continue;

      let updatedChildTable = childTable;
      const newlyCreatedFkColumns = [];

      for (const pkColumn of pkColumns) {
        const relColumn = rel.columns.find(
          (rc) => rc.pkColumnId === pkColumn.id,
        );

        if (relColumn && relColumn.fkColumnId && relColumn.fkColumnId !== "") {
          const existingColumn = childTable.columns.find(
            (c) => c.id === relColumn.fkColumnId,
          );
          if (existingColumn) continue;
        }

        const newColumnId =
          relColumn?.fkColumnId && relColumn.fkColumnId !== ""
            ? relColumn.fkColumnId
            : `${ulid()}`;
        const columnName = `${parentTable.name}_${pkColumn.name}`;

        const newColumn: Column = {
          ...pkColumn,
          isAffected: true,
          id: newColumnId,
          tableId: childTable.id,
          name: columnName,
          seqNo: childTable.columns.length,
        };

        updatedChildTable = {
          ...updatedChildTable,
          isAffected: true,
          columns: [...updatedChildTable.columns, newColumn],
        };

        newlyCreatedFkColumns.push(newColumn);

        if (!relColumn) {
          const newRelColumn: RelationshipColumn = {
            id: `${ulid()}`,
            relationshipId: rel.id,
            fkColumnId: newColumnId,
            pkColumnId: pkColumn.id,
            seqNo: rel.columns.length,
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
            relationships: table.relationships.map((r) =>
              r.id === rel.id ? updatedRel : r,
            ),
          };

          updatedSchema = {
            ...updatedSchema,
            isAffected: true,
            tables: updatedSchema.tables.map((t) =>
              t.id === updatedRelHolderTable.id ? updatedRelHolderTable : t,
            ),
          };
        }
      }

      updatedSchema = {
        ...updatedSchema,
        isAffected: true,
        tables: updatedSchema.tables.map((t) =>
          t.id === childTable.id ? updatedChildTable : t,
        ),
      };

      if (rel.kind === "NON_IDENTIFYING") continue;

      if (newlyCreatedFkColumns.length <= 0) continue;

      const childPkConstraint = updatedChildTable.constraints.find(
        (c) => c.kind === "PRIMARY_KEY",
      );

      if (childPkConstraint) {
        const newPkColumns = [];

        for (const fkColumn of newlyCreatedFkColumns) {
          newPkColumns.push({
            id: `${ulid()}`,
            isAffected: true,
            constraintId: childPkConstraint.id,
            columnId: fkColumn.id,
            seqNo: childPkConstraint.columns.length + newPkColumns.length,
          });
        }

        const updatedPkConstraint = {
          ...childPkConstraint,
          isAffected: true,
          columns: [...childPkConstraint.columns, ...newPkColumns],
        };

        updatedChildTable = {
          ...updatedChildTable,
          isAffected: true,
          constraints: updatedChildTable.constraints.map((c) =>
            c.id === childPkConstraint.id ? updatedPkConstraint : c,
          ),
        };

        updatedSchema = {
          ...updatedSchema,
          isAffected: true,
          tables: updatedSchema.tables.map((t) =>
            t.id === childTable.id ? updatedChildTable : t,
          ),
        };
      } else {
        const newPkConstraintId = `${ulid()}`;
        const newPkColumns = [];

        for (const fkColumn of newlyCreatedFkColumns) {
          newPkColumns.push({
            id: `${ulid()}`,
            isAffected: true,
            constraintId: newPkConstraintId,
            columnId: fkColumn.id,
            seqNo: newPkColumns.length,
          });
        }

        const newPkConstraint: Constraint = {
          id: newPkConstraintId,
          name: `pk_${childTable.name}`,
          columns: newPkColumns,
          tableId: childTable.id,
          kind: "PRIMARY_KEY",
          isAffected: true,
        };

        updatedChildTable = {
          ...updatedChildTable,
          isAffected: true,
          constraints: [...updatedChildTable.constraints, newPkConstraint],
        };

        updatedSchema = {
          ...updatedSchema,
          isAffected: true,
          tables: updatedSchema.tables.map((t) =>
            t.id === childTable.id ? updatedChildTable : t,
          ),
        };
      }

      updatedSchema = propagateKeysToChildren(
        structuredClone(updatedSchema),
        rel.fkTableId,
        new Set(visited),
      );
    }
  }

  return updatedSchema;
};

export const deleteRelatedColumns = (
  currentSchema: Schema,
  relationshipToDelete: Relationship,
  visited: Set<string> = new Set(),
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
      if (table.id === relationshipToDelete.fkTableId) {
        const isAffected =
          table.relationships.some((r) => r.id === relationshipToDelete.id) ||
          table.columns.some((col) => fkColumnsToDelete.has(col.id)) ||
          table.indexes.some((idx) =>
            idx.columns.some((ic) => fkColumnsToDelete.has(ic.columnId)),
          ) ||
          table.constraints.some((constraint) =>
            constraint.columns.some((cc) => fkColumnsToDelete.has(cc.columnId)),
          );
        return {
          ...table,
          isAffected,
          relationships: table.relationships.filter(
            (r) => r.id !== relationshipToDelete.id,
          ),
          columns: table.columns.filter(
            (col) => !fkColumnsToDelete.has(col.id),
          ),
          indexes: table.indexes.map((idx) => ({
            ...idx,
            columns: idx.columns.filter(
              (ic) => !fkColumnsToDelete.has(ic.columnId),
            ),
          })),
          constraints: table.constraints
            .map((constraint) => {
              const remainingColumns = constraint.columns.filter(
                (cc) => !fkColumnsToDelete.has(cc.columnId),
              );

              const finalColumns =
                constraint.kind === "PRIMARY_KEY"
                  ? remainingColumns.map((cc, index) => ({
                      ...cc,
                      seqNo: index,
                      isAffected: true,
                    }))
                  : remainingColumns;

              return {
                ...constraint,
                columns: finalColumns,
              };
            })
            .filter((constraint) => constraint.columns.length > 0),
        };
      }
      return table;
    }),
  };

  if (relationshipToDelete.kind === "IDENTIFYING") {
    for (const table of updatedSchema.tables) {
      const relationshipsToDelete = table.relationships.filter((rel) => {
        return rel.columns.some((relCol) =>
          fkColumnsToDelete.has(relCol.pkColumnId),
        );
      });

      for (const relToDelete of relationshipsToDelete) {
        if (!visited.has(relToDelete.id)) {
          updatedSchema = deleteRelatedColumns(
            structuredClone(updatedSchema),
            relToDelete,
            new Set(visited),
          );
        }
      }
    }
  }

  return updatedSchema;
};
