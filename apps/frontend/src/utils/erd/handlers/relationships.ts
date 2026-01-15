import { ulid } from "ulid";
import {
  SchemaNotExistError,
  RelationshipNotExistError,
  RelationshipTargetTableNotExistError,
  RelationshipColumnNotExistError,
  RelationshipNameNotUniqueError,
  RelationshipEmptyError,
  RelationshipCyclicReferenceError,
} from "../errors";
import type {
  Database,
  Schema,
  Relationship,
  RelationshipColumn,
  Constraint,
  Table,
} from "@/types/erd.types";
import * as helper from "../helper";

export interface RelationshipHandlers {
  createRelationship: (
    database: Database,
    schemaId: Schema["id"],
    relationship: Relationship,
  ) => Database;
  deleteRelationship: (
    database: Database,
    schemaId: Schema["id"],
    relationshipId: Relationship["id"],
  ) => Database;
  changeRelationshipName: (
    database: Database,
    schemaId: Schema["id"],
    relationshipId: Relationship["id"],
    newName: Relationship["name"],
  ) => Database;
  changeRelationshipCardinality: (
    database: Database,
    schemaId: Schema["id"],
    relationshipId: Relationship["id"],
    cardinality: Relationship["cardinality"],
  ) => Database;
  changeRelationshipKind: (
    database: Database,
    schemaId: Schema["id"],
    relationshipId: Relationship["id"],
    kind: Relationship["kind"],
  ) => Database;
  addColumnToRelationship: (
    database: Database,
    schemaId: Schema["id"],
    relationshipId: Relationship["id"],
    relationshipColumn: Omit<RelationshipColumn, "relationshipId">,
  ) => Database;
  removeColumnFromRelationship: (
    database: Database,
    schemaId: Schema["id"],
    relationshipId: Relationship["id"],
    relationshipColumnId: RelationshipColumn["id"],
  ) => Database;
}

export const relationshipHandlers: RelationshipHandlers = {
  createRelationship: (database, schemaId, relationship) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    if (!relationship.columns || relationship.columns.length === 0) {
      throw new RelationshipEmptyError(relationship.name);
    }

    const fkTable = schema.tables.find((t) => t.id === relationship.fkTableId);
    if (!fkTable)
      throw new RelationshipTargetTableNotExistError(
        relationship.name,
        relationship.fkTableId,
      );

    const pkTable = schema.tables.find((t) => t.id === relationship.pkTableId);
    if (!pkTable)
      throw new RelationshipTargetTableNotExistError(
        relationship.name,
        relationship.pkTableId,
      );

    if (relationship.kind === "IDENTIFYING") {
      const cycle = helper.detectIdentifyingCycleInSchema(schema, undefined, {
        fkTableId: relationship.fkTableId,
        pkTableId: relationship.pkTableId,
        kind: relationship.kind,
      });
      if (cycle) {
        throw new RelationshipCyclicReferenceError(cycle[0], cycle[1]);
      }
    }

    const duplicateRelationship = fkTable.relationships.find(
      (r) => r.name === relationship.name,
    );
    if (duplicateRelationship)
      throw new RelationshipNameNotUniqueError(relationship.name, fkTable.id);

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === relationship.fkTableId
        ? {
            ...t,
            isAffected: true,
            relationships: [
              ...t.relationships,
              {
                ...relationship,
                isAffected: true,
                columns: relationship.columns.map((column) => ({
                  ...column,
                  relationshipId: relationship.id,
                  isAffected: true,
                })),
              },
            ],
          }
        : t,
    );

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId
        ? {
            ...s,
            isAffected: true,
            tables: changeTables,
          }
        : s,
    );

    let updatedDatabase = {
      ...database,
      isAffected: true,
      schemas: changeSchemas,
    };

    const updatedSchema = changeSchemas.find((s) => s.id === schemaId)!;
    const propagatedSchema = helper.propagateKeysToChildren(
      structuredClone(updatedSchema),
      relationship.pkTableId,
    );

    updatedDatabase = {
      ...updatedDatabase,
      isAffected: true,
      schemas: updatedDatabase.schemas.map((s) =>
        s.id === schemaId ? { ...propagatedSchema, isAffected: true } : s,
      ),
    };

    return updatedDatabase;
  },
  deleteRelationship: (database, schemaId, relationshipId) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const relationship: Relationship | undefined = schema.tables
      .find((t) => t.relationships.some((r) => r.id === relationshipId))
      ?.relationships.find((r) => r.id === relationshipId);

    if (!relationship) throw new RelationshipNotExistError(relationshipId);

    const updatedSchema = helper.deleteRelatedColumns(
      structuredClone(schema),
      relationship,
    );

    const deleteRelationshipSchema: Schema = {
      ...updatedSchema,
      isAffected: true,
      tables: updatedSchema.tables.map((t) => ({
        ...t,
        isAffected:
          t.relationships.some((r) => r.id === relationshipId) || t.isAffected,
        relationships: t.relationships.filter((r) => r.id !== relationshipId),
      })),
    };

    return {
      ...structuredClone(database),
      isAffected: true,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? { ...deleteRelationshipSchema, isAffected: true }
          : s,
      ),
    };
  },
  changeRelationshipName: (database, schemaId, relationshipId, newName) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const relationship: Relationship | undefined = schema.tables
      .find((t) => t.relationships.some((r) => r.id === relationshipId))
      ?.relationships.find((r) => r.id === relationshipId);

    if (!relationship) throw new RelationshipNotExistError(relationshipId);

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === relationship.fkTableId || t.id === relationship.pkTableId
        ? {
            ...t,
            isAffected: true,
            relationships: t.relationships.map((r) =>
              r.id === relationshipId
                ? { ...r, name: newName, isAffected: true }
                : r,
            ),
          }
        : t,
    );

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId
        ? {
            ...s,
            isAffected: true,
            tables: changeTables,
          }
        : s,
    );

    return {
      ...database,
      isAffected: true,
      schemas: changeSchemas,
    };
  },
  changeRelationshipCardinality: (
    database,
    schemaId,
    relationshipId,
    cardinality,
  ) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const relationship: Relationship | undefined = schema.tables
      .find((t) => t.relationships.some((r) => r.id === relationshipId))
      ?.relationships.find((r) => r.id === relationshipId);

    if (!relationship) throw new RelationshipNotExistError(relationshipId);

    const updatedRelationship: Relationship = {
      ...relationship,
      isAffected: true,
      cardinality,
    };

    let currentDatabase = relationshipHandlers.deleteRelationship(
      structuredClone(database),
      schemaId,
      relationshipId,
    );

    currentDatabase = relationshipHandlers.createRelationship(
      structuredClone(currentDatabase),
      schemaId,
      updatedRelationship,
    );

    return currentDatabase;
  },
  changeRelationshipKind: (database, schemaId, relationshipId, kind) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const relationship: Relationship | undefined = schema.tables
      .find((t) => t.relationships.some((r) => r.id === relationshipId))
      ?.relationships.find((r) => r.id === relationshipId);

    if (!relationship) throw new RelationshipNotExistError(relationshipId);

    if (kind === "IDENTIFYING") {
      const cycle = helper.detectIdentifyingCycleInSchema(schema, {
        relationshipId,
        newKind: kind,
      });
      if (cycle) {
        throw new RelationshipCyclicReferenceError(cycle[0], cycle[1]);
      }
    }

    const updatedRelationship: Relationship = {
      ...relationship,
      isAffected: true,
      kind,
    };

    const fkColumnIds = relationship.columns.map((rc) => rc.fkColumnId);
    let addedPkColumnIds: string[] = [];

    const changeTables: Table[] = schema.tables.map((t) => {
      if (t.id !== relationship.fkTableId) return t;

      const updatedRelationships = t.relationships.map((r) =>
        r.id === relationshipId ? updatedRelationship : r,
      );

      let updatedConstraints = t.constraints;

      if (kind === "IDENTIFYING") {
        const pkConstraint = t.constraints.find(
          (c) => c.kind === "PRIMARY_KEY",
        );
        const existingPkColumnIds = new Set(
          pkConstraint?.columns.map((cc) => cc.columnId) ?? [],
        );
        addedPkColumnIds = fkColumnIds.filter(
          (columnId) => !existingPkColumnIds.has(columnId),
        );

        if (addedPkColumnIds.length > 0) {
          if (pkConstraint) {
            const newPkColumns = addedPkColumnIds.map((columnId, index) => ({
              id: `${ulid()}`,
              isAffected: true,
              constraintId: pkConstraint.id,
              columnId,
              seqNo: pkConstraint.columns.length + index,
            }));

            const updatedPkConstraint: Constraint = {
              ...pkConstraint,
              isAffected: true,
              columns: [...pkConstraint.columns, ...newPkColumns],
            };

            updatedConstraints = t.constraints.map((c) =>
              c.id === pkConstraint.id ? updatedPkConstraint : c,
            );
          } else {
            const newPkConstraintId = `${ulid()}`;
            const newPkColumns = addedPkColumnIds.map((columnId, index) => ({
              id: `${ulid()}`,
              isAffected: true,
              constraintId: newPkConstraintId,
              columnId,
              seqNo: index,
            }));

            const newPkConstraint: Constraint = {
              id: newPkConstraintId,
              name: `pk_${t.name}`,
              columns: newPkColumns,
              tableId: t.id,
              kind: "PRIMARY_KEY",
              isAffected: true,
            };

            updatedConstraints = [...t.constraints, newPkConstraint];
          }
        }
      } else {
        const pkConstraint = t.constraints.find(
          (c) => c.kind === "PRIMARY_KEY",
        );
        if (pkConstraint) {
          const remainingColumns = pkConstraint.columns.filter(
            (cc) => !fkColumnIds.includes(cc.columnId),
          );

          if (remainingColumns.length !== pkConstraint.columns.length) {
            if (remainingColumns.length === 0) {
              updatedConstraints = t.constraints.filter(
                (c) => c.id !== pkConstraint.id,
              );
            } else {
              const resequencedColumns = remainingColumns.map(
                (column, index) => ({
                  ...column,
                  seqNo: index,
                  isAffected: true,
                }),
              );

              const updatedPkConstraint: Constraint = {
                ...pkConstraint,
                isAffected: true,
                columns: resequencedColumns,
              };

              updatedConstraints = t.constraints.map((c) =>
                c.id === pkConstraint.id ? updatedPkConstraint : c,
              );
            }
          }
        }
      }

      return {
        ...t,
        isAffected: true,
        relationships: updatedRelationships,
        constraints: updatedConstraints,
      };
    });

    let updatedSchema: Schema = {
      ...schema,
      isAffected: true,
      tables: changeTables,
    };

    if (kind === "IDENTIFYING" && addedPkColumnIds.length > 0) {
      for (const columnId of addedPkColumnIds) {
        const tableWithNewPk = updatedSchema.tables.find(
          (t) => t.id === relationship.fkTableId,
        );
        const newPkColumn = tableWithNewPk?.columns.find(
          (c) => c.id === columnId,
        );
        if (!newPkColumn) continue;

        updatedSchema = helper.propagateNewPrimaryKey(
          structuredClone(updatedSchema),
          relationship.fkTableId,
          newPkColumn,
        );
      }
    }

    return {
      ...database,
      isAffected: true,
      schemas: database.schemas.map((s) =>
        s.id === schemaId ? { ...updatedSchema, isAffected: true } : s,
      ),
    };
  },
  addColumnToRelationship: (
    database,
    schemaId,
    relationshipId,
    relationshipColumn,
  ) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const relationship: Relationship | undefined = schema.tables
      .find((t) => t.relationships.some((r) => r.id === relationshipId))
      ?.relationships.find((r) => r.id === relationshipId);
    if (!relationship) throw new RelationshipNotExistError(relationshipId);

    const updatedRelationship: Relationship = {
      ...relationship,
      isAffected: true,
      columns: [
        ...relationship.columns,
        { ...relationshipColumn, relationshipId, isAffected: true },
      ],
    };

    let currentDatabase = relationshipHandlers.deleteRelationship(
      structuredClone(database),
      schemaId,
      relationshipId,
    );

    currentDatabase = relationshipHandlers.createRelationship(
      structuredClone(currentDatabase),
      schemaId,
      updatedRelationship,
    );

    return currentDatabase;
  },
  removeColumnFromRelationship: (
    database,
    schemaId,
    relationshipId,
    relationshipColumnId,
  ) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const relationship: Relationship | undefined = schema.tables
      .find((t) => t.relationships.some((r) => r.id === relationshipId))
      ?.relationships.find((r) => r.id === relationshipId);

    if (!relationship) throw new RelationshipNotExistError(relationshipId);

    const relationshipColumn = relationship.columns.find(
      (rc) => rc.id === relationshipColumnId,
    );
    if (!relationshipColumn)
      throw new RelationshipColumnNotExistError(
        relationshipColumnId,
        relationshipId,
      );

    const nextColumns = relationship.columns.filter(
      (rc) => rc.id !== relationshipColumnId,
    );

    // 마지막 컬럼 제거 시 관계 자체를 삭제
    if (nextColumns.length === 0) {
      return relationshipHandlers.deleteRelationship(
        structuredClone(database),
        schemaId,
        relationshipId,
      );
    }

    const updatedRelationship: Relationship = {
      ...relationship,
      isAffected: true,
      columns: nextColumns,
    };

    let currentDatabase = relationshipHandlers.deleteRelationship(
      structuredClone(database),
      schemaId,
      relationshipId,
    );

    currentDatabase = relationshipHandlers.createRelationship(
      structuredClone(currentDatabase),
      schemaId,
      updatedRelationship,
    );

    return currentDatabase;
  },
};
