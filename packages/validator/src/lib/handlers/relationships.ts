import {
    SchemaNotExistError,
    RelationshipNotExistError,
    RelationshipTargetTableNotExistError,
    RelationshipColumnNotExistError,
    RelationshipNameNotUniqueError,
    RelationshipEmptyError,
    RelationshipCyclicReferenceError,
} from "../errors";
import type { Database, Schema, Relationship, RelationshipColumn } from "../types";
import * as helper from "../helper";

export interface RelationshipHandlers {
    createRelationship: (database: Database, schemaId: Schema["id"], relationship: Relationship) => Database;
    deleteRelationship: (database: Database, schemaId: Schema["id"], relationshipId: Relationship["id"]) => Database;
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

        const sourceTable = schema.tables.find((t) => t.id === relationship.srcTableId);
        if (!sourceTable) throw new RelationshipTargetTableNotExistError(relationship.srcTableId, schemaId);

        const targetTable = schema.tables.find((t) => t.id === relationship.tgtTableId);
        if (!targetTable) throw new RelationshipTargetTableNotExistError(relationship.tgtTableId, schemaId);

        if (
            relationship.kind === "IDENTIFYING" &&
            helper.detectCircularReference(schema, relationship.tgtTableId, relationship.srcTableId)
        )
            throw new RelationshipCyclicReferenceError(relationship.tgtTableId, relationship.srcTableId);

        const duplicateRelationship = sourceTable.relationships.find((r) => r.name === relationship.name);
        if (duplicateRelationship) throw new RelationshipNameNotUniqueError(relationship.name);

        let updatedDatabase = {
            ...database,
            schemas: database.schemas.map((s) =>
                s.id === schemaId
                    ? {
                          ...s,
                          tables: s.tables.map((t) =>
                              t.id === relationship.srcTableId
                                  ? {
                                        ...t,
                                        relationships: [...t.relationships, relationship],
                                    }
                                  : t,
                          ),
                      }
                    : s,
            ),
        };

        const updatedSchema = updatedDatabase.schemas.find((s) => s.id === schemaId)!;
        const pkConstraint = targetTable.constraints.find((c) => c.kind === "PRIMARY_KEY");

        if (pkConstraint) {
            const pkColumnIds = pkConstraint.columns.map((cc) => cc.columnId);
            const pkColumns = targetTable.columns.filter((col) => pkColumnIds.includes(col.id));

            const propagatedSchema = helper.propagatePrimaryKeysToChildren(
                structuredClone(updatedSchema),
                relationship.tgtTableId,
                pkColumns,
            );

            updatedDatabase = {
                ...updatedDatabase,
                schemas: updatedDatabase.schemas.map((s) => (s.id === schemaId ? propagatedSchema : s)),
            };
        }

        return updatedDatabase;
    },
    deleteRelationship: (database, schemaId, relationshipId) => {
        const schema = database.schemas.find((s) => s.id === schemaId);
        if (!schema) throw new SchemaNotExistError(schemaId);

        let foundRelationship = null;
        for (const table of schema.tables) {
            const relationship = table.relationships.find((r) => r.id === relationshipId);
            if (relationship) {
                foundRelationship = relationship;
                break;
            }
        }

        if (!foundRelationship) throw new RelationshipNotExistError(relationshipId);

        const deleteRelatedColumns = (
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
                tables: updatedSchema.tables.map((table) => {
                    if (table.id === relationshipToDelete.srcTableId) {
                        return {
                            ...table,
                            relationships: table.relationships.filter((r) => r.id !== relationshipToDelete.id),
                            columns: table.columns.filter((col) => !fkColumnsToDelete.has(col.id)),
                            indexes: table.indexes
                                .map((idx) => ({
                                    ...idx,
                                    columns: idx.columns.filter((ic) => !fkColumnsToDelete.has(ic.columnId)),
                                }))
                                .filter((idx) => idx.columns.length > 0),
                            constraints: table.constraints
                                .map((constraint) => ({
                                    ...constraint,
                                    columns: constraint.columns.filter((cc) => !fkColumnsToDelete.has(cc.columnId)),
                                }))
                                .filter((constraint) => constraint.columns.length > 0),
                        };
                    }
                    return table;
                }),
            };

            if (relationshipToDelete.kind === "IDENTIFYING") {
                for (const table of updatedSchema.tables) {
                    const relationshipsToDelete = table.relationships.filter((rel) => {
                        return rel.columns.some((relCol) => fkColumnsToDelete.has(relCol.refColumnId));
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

        const updatedSchema = deleteRelatedColumns(structuredClone(schema), foundRelationship);

        return {
            ...database,
            schemas: database.schemas.map((s) => (s.id === schemaId ? updatedSchema : s)),
        };
    },
    changeRelationshipName: (database, schemaId, relationshipId, newName) => {
        const schema = database.schemas.find((s) => s.id === schemaId);
        if (!schema) throw new SchemaNotExistError(schemaId);

        let foundRelationship = null;
        let sourceTableId = null;
        for (const table of schema.tables) {
            const relationshipNotUnique = table.relationships.find((r) => r.name === newName);
            if (relationshipNotUnique) throw new RelationshipNameNotUniqueError(newName);

            const relationship = table.relationships.find((r) => r.id === relationshipId);
            if (relationship) {
                foundRelationship = relationship;
                sourceTableId = table.id;
                break;
            }
        }

        if (!foundRelationship) throw new RelationshipNotExistError(relationshipId);

        return {
            ...database,
            schemas: database.schemas.map((s) =>
                s.id === schemaId
                    ? {
                          ...s,
                          tables: s.tables.map((t) =>
                              t.id === sourceTableId
                                  ? {
                                        ...t,
                                        relationships: t.relationships.map((r) =>
                                            r.id === relationshipId ? { ...r, name: newName } : r,
                                        ),
                                    }
                                  : t,
                          ),
                      }
                    : s,
            ),
        };
    },
    changeRelationshipCardinality: (database, schemaId, relationshipId, cardinality) => {
        const schema = database.schemas.find((s) => s.id === schemaId);
        if (!schema) throw new SchemaNotExistError(schemaId);

        let foundRelationship = null;
        for (const table of schema.tables) {
            const relationship = table.relationships.find((r) => r.id === relationshipId);
            if (relationship) {
                foundRelationship = relationship;
                break;
            }
        }

        if (!foundRelationship) throw new RelationshipNotExistError(relationshipId);

        const updatedRelationship = {
            ...foundRelationship,
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
    addColumnToRelationship: (database, schemaId, relationshipId, relationshipColumn) => {
        const schema = database.schemas.find((s) => s.id === schemaId);
        if (!schema) throw new SchemaNotExistError(schemaId);

        let foundRelationship = null;
        let sourceTableId = null;
        for (const table of schema.tables) {
            const relationship = table.relationships.find((r) => r.id === relationshipId);
            if (relationship) {
                foundRelationship = relationship;
                sourceTableId = table.id;
                break;
            }
        }

        if (!foundRelationship) throw new RelationshipNotExistError(relationshipId);

        return {
            ...database,
            schemas: database.schemas.map((s) =>
                s.id === schemaId
                    ? {
                          ...s,
                          tables: s.tables.map((t) =>
                              t.id === sourceTableId
                                  ? {
                                        ...t,
                                        relationships: t.relationships.map((r) =>
                                            r.id === relationshipId
                                                ? {
                                                      ...r,
                                                      columns: [
                                                          ...r.columns,
                                                          { ...relationshipColumn, relationshipId },
                                                      ],
                                                  }
                                                : r,
                                        ),
                                    }
                                  : t,
                          ),
                      }
                    : s,
            ),
        };
    },
    removeColumnFromRelationship: (database, schemaId, relationshipId, relationshipColumnId) => {
        const schema = database.schemas.find((s) => s.id === schemaId);
        if (!schema) throw new SchemaNotExistError(schemaId);

        let foundRelationship = null;
        let sourceTableId = null;
        for (const table of schema.tables) {
            const relationship = table.relationships.find((r) => r.id === relationshipId);
            if (relationship) {
                foundRelationship = relationship;
                sourceTableId = table.id;
                break;
            }
        }

        if (!foundRelationship) throw new RelationshipNotExistError(relationshipId);

        const relationshipColumn = foundRelationship.columns.find((rc) => rc.id === relationshipColumnId);
        if (!relationshipColumn) throw new RelationshipColumnNotExistError(relationshipColumnId);

        return {
            ...database,
            schemas: database.schemas.map((s) =>
                s.id === schemaId
                    ? {
                          ...s,
                          tables: s.tables.map((t) =>
                              t.id === sourceTableId
                                  ? {
                                        ...t,
                                        relationships: t.relationships
                                            .map((r) =>
                                                r.id === relationshipId
                                                    ? {
                                                          ...r,
                                                          columns: r.columns.filter(
                                                              (rc) => rc.id !== relationshipColumnId,
                                                          ),
                                                      }
                                                    : r,
                                            )
                                            .filter((r) => r.columns.length > 0),
                                    }
                                  : t,
                          ),
                      }
                    : s,
            ),
        };
    },
};
