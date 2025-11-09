import {
  SchemaNotExistError,
  RelationshipNotExistError,
  RelationshipTargetTableNotExistError,
  RelationshipColumnNotExistError,
  RelationshipNameNotUniqueError,
  RelationshipEmptyError,
  RelationshipCyclicReferenceError,
} from "../errors";
import {
  Database,
  Schema,
  Relationship,
  RelationshipColumn,
  Table,
} from "../types";
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

    const sourceTable = schema.tables.find(
      (t) => t.id === relationship.srcTableId,
    );
    if (!sourceTable)
      throw new RelationshipTargetTableNotExistError(
        relationship.name,
        relationship.srcTableId,
      );

    const targetTable = schema.tables.find(
      (t) => t.id === relationship.tgtTableId,
    );
    if (!targetTable)
      throw new RelationshipTargetTableNotExistError(
        relationship.name,
        relationship.tgtTableId,
      );

    if (
      relationship.kind === "IDENTIFYING" &&
      helper.detectCircularReference(
        schema,
        relationship.tgtTableId,
        relationship.srcTableId,
      )
    ) {
      throw new RelationshipCyclicReferenceError(
        relationship.tgtTableId,
        relationship.srcTableId,
      );
    }

    const duplicateRelationship = sourceTable.relationships.find(
      (r) => r.name === relationship.name,
    );
    if (duplicateRelationship)
      throw new RelationshipNameNotUniqueError(
        relationship.name,
        sourceTable.id,
      );

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === relationship.srcTableId
        ? {
            ...t,
            isAffected: true,
            relationships: [
              ...t.relationships,
              { ...relationship, isAffected: true },
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
      relationship.tgtTableId,
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
      tables: schema.tables.map((t) => ({
        ...t,
        isAffected: t.relationships.some((r) => r.id === relationshipId),
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
      t.id === relationship.srcTableId || t.id === relationship.tgtTableId
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
