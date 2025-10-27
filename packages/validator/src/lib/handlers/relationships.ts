import {
    SchemaNotExistError,
    RelationshipNotExistError,
    RelationshipTargetTableNotExistError,
    RelationshipColumnNotExistError,
    RelationshipNameNotUniqueError,
    RelationshipEmptyError,
    RelationshipCyclicReferenceError,
} from "../errors";
import type { Database, Schema, Relationship, RelationshipColumn, Table } from "../types";
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

        const propagateKeysToChildren = (
            currentSchema: Schema,
            parentTableId: Table["id"],
            visited: Set<Table["id"]> = new Set(),
        ): Schema => {
            if (visited.has(parentTableId)) return currentSchema;
            visited.add(parentTableId);

            const parentTable = currentSchema.tables.find((t) => t.id === parentTableId);
            if (!parentTable) return currentSchema;

            const pkConstraint = parentTable.constraints.find((c) => c.kind === "PRIMARY_KEY");
            if (!pkConstraint) return currentSchema;

            const pkColumnIds = pkConstraint.columns.map((cc) => cc.columnId);
            const pkColumns = parentTable.columns.filter((col) => pkColumnIds.includes(col.id));

            let updatedSchema = currentSchema;

            for (const table of updatedSchema.tables) {
                for (const rel of table.relationships) {
                    if (rel.tgtTableId === parentTableId) {
                        const childTable = updatedSchema.tables.find((t) => t.id === rel.srcTableId);
                        if (!childTable) continue;

                        let updatedChildTable = childTable;
                        const newlyCreatedFkColumns = [];

                        for (const pkColumn of pkColumns) {
                            const relColumn = rel.columns.find((rc) => rc.refColumnId === pkColumn.id);

                            if (relColumn && relColumn.fkColumnId) {
                                const existingColumn = childTable.columns.find((c) => c.id === relColumn.fkColumnId);
                                if (existingColumn) continue;
                            }

                            const newColumnId = `fkcol_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
                            const columnName = `${parentTable.name}_${pkColumn.name}`;

                            const newColumn = {
                                ...pkColumn,
                                id: newColumnId,
                                tableId: childTable.id,
                                name: columnName,
                                ordinalPosition: childTable.columns.length + 1,
                                createdAt: new Date(),
                                updatedAt: new Date(),
                            };

                            updatedChildTable = {
                                ...updatedChildTable,
                                columns: [...updatedChildTable.columns, newColumn],
                            };

                            newlyCreatedFkColumns.push(newColumn);

                            if (relColumn) {
                                // relColumn이 있으면 기존 relColumn의 fkColumnId만 업데이트
                                const updatedRel = {
                                    ...rel,
                                    columns: rel.columns.map((rc) =>
                                        rc.id === relColumn.id ? { ...rc, fkColumnId: newColumnId } : rc,
                                    ),
                                };

                                updatedChildTable = {
                                    ...updatedChildTable,
                                    relationships: updatedChildTable.relationships.map((r) =>
                                        r.id === rel.id ? updatedRel : r,
                                    ),
                                };
                            } else {
                                // relColumn이 없으면 새 relColumn 생성
                                const newRelColumn = {
                                    id: `rel_col_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
                                    relationshipId: rel.id,
                                    fkColumnId: newColumnId,
                                    refColumnId: pkColumn.id,
                                    seqNo: rel.columns.length + 1,
                                };

                                const updatedRel = {
                                    ...rel,
                                    columns: [...rel.columns, newRelColumn],
                                };

                                updatedChildTable = {
                                    ...updatedChildTable,
                                    relationships: updatedChildTable.relationships.map((r) =>
                                        r.id === rel.id ? updatedRel : r,
                                    ),
                                };
                            }
                        }

                        updatedSchema = {
                            ...updatedSchema,
                            tables: updatedSchema.tables.map((t) => (t.id === childTable.id ? updatedChildTable : t)),
                        };

                        if (rel.kind === "IDENTIFYING") {
                            const childPkConstraint = updatedChildTable.constraints.find(
                                (c) => c.kind === "PRIMARY_KEY",
                            );
                            if (childPkConstraint && newlyCreatedFkColumns.length > 0) {
                                const newPkColumns = [];

                                for (const fkColumn of newlyCreatedFkColumns) {
                                    newPkColumns.push({
                                        id: `constraint_col_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
                                        constraintId: childPkConstraint.id,
                                        columnId: fkColumn.id,
                                        seqNo: childPkConstraint.columns.length + newPkColumns.length + 1,
                                    });
                                }

                                if (newPkColumns.length > 0) {
                                    const updatedPkConstraint = {
                                        ...childPkConstraint,
                                        columns: [...childPkConstraint.columns, ...newPkColumns],
                                    };

                                    updatedChildTable = {
                                        ...updatedChildTable,
                                        constraints: updatedChildTable.constraints.map((c) =>
                                            c.id === childPkConstraint.id ? updatedPkConstraint : c,
                                        ),
                                    };

                                    updatedSchema = {
                                        ...updatedSchema,
                                        tables: updatedSchema.tables.map((t) =>
                                            t.id === childTable.id ? updatedChildTable : t,
                                        ),
                                    };
                                }
                            }

                            updatedSchema = propagateKeysToChildren(
                                structuredClone(updatedSchema),
                                rel.tgtTableId,
                                new Set(visited),
                            );
                        }
                    }
                }
            }

            return updatedSchema;
        };

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
        const propagatedSchema = propagateKeysToChildren(structuredClone(updatedSchema), relationship.tgtTableId);

        updatedDatabase = {
            ...updatedDatabase,
            schemas: updatedDatabase.schemas.map((s) => (s.id === schemaId ? propagatedSchema : s)),
        };

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
