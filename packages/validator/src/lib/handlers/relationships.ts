import {
  SchemaNotExistError,
  TableNotExistError,
  ColumnNotExistError,
  RelationshipNotExistError,
  RelationshipNameInvalidError,
  RelationshipColumnNotExistError,
} from '../errors';
import { Database, RELATIONSHIP, Schema, Relationship, RelationshipColumn } from '../types';
import * as helper from '../helper';

export interface RelationshipHandlers {
  createRelationship: (database: Database, schemaId: Schema['id'], relationship: Relationship) => Database;
  deleteRelationship: (database: Database, schemaId: Schema['id'], relationshipId: Relationship['id']) => Database;
  changeRelationshipName: (
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    newName: Relationship['name']
  ) => Database;
  changeRelationshipCardinality: (
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    cardinality: Relationship['cardinality']
  ) => Database;
  addColumnToRelationship: (
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    relationshipColumn: Omit<RelationshipColumn, 'relationshipId'>
  ) => Database;
  removeColumnFromRelationship: (
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    relationshipColumnId: RelationshipColumn['id']
  ) => Database;
}

export const relationshipHandlers: RelationshipHandlers = {
  createRelationship: (database, schemaId, relationship) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const sourceTable = schema.tables.find((t) => t.id === relationship.srcTableId);
    if (!sourceTable) throw new TableNotExistError(relationship.srcTableId);

    const targetTable = schema.tables.find((t) => t.id === relationship.tgtTableId);
    if (!targetTable) throw new TableNotExistError(relationship.tgtTableId);

    // Check for circular references
    if (helper.detectCircularReference(schema, relationship.tgtTableId, relationship.srcTableId)) {
      throw new Error(
        `Creating this relationship would cause a circular reference between tables ${relationship.srcTableId} and ${relationship.tgtTableId}`
      );
    }

    // Validate column type compatibility
    for (const relColumn of relationship.columns) {
      const srcColumn = sourceTable.columns.find((c) => c.id === relColumn.srcColumnId);
      const tgtColumn = targetTable.columns.find((c) => c.id === relColumn.tgtColumnId);

      if (!srcColumn) throw new ColumnNotExistError(relColumn.srcColumnId);
      if (!tgtColumn) throw new ColumnNotExistError(relColumn.tgtColumnId);

      if (!helper.validateColumnTypeCompatibility(srcColumn, tgtColumn)) {
        throw new Error(`Column types are not compatible: ${srcColumn.dataType} vs ${tgtColumn.dataType}`);
      }
    }

    const result = RELATIONSHIP.omit({ id: true }).safeParse(relationship);
    if (!result.success) throw new RelationshipNameInvalidError(relationship.name);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === relationship.srcTableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
                      relationships: [...t.relationships, relationship],
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  deleteRelationship: (database, schemaId, relationshipId) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    // Find the relationship in any table of the schema
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
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === sourceTableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
                      relationships: t.relationships.filter((r) => r.id !== relationshipId),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  changeRelationshipName: (database, schemaId, relationshipId, newName) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    // Find the relationship in any table of the schema
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

    const result = RELATIONSHIP.shape.name.safeParse(newName);
    if (!result.success) throw new RelationshipNameInvalidError(newName);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === sourceTableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
                      relationships: t.relationships.map((r) =>
                        r.id === relationshipId ? { ...r, name: newName } : r
                      ),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  changeRelationshipCardinality: (database, schemaId, relationshipId, cardinality) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    // Find the relationship in any table of the schema
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

    // Validate cardinality change with column constraints
    if (cardinality === '1:1') {
      // For 1:1 relationships, FK columns should be NOT NULL
      const sourceTable = schema.tables.find((t) => t.id === foundRelationship.srcTableId);
      const targetTable = schema.tables.find((t) => t.id === foundRelationship.tgtTableId);

      if (sourceTable && targetTable) {
        // Check if FK columns are nullable
        for (const relColumn of foundRelationship.columns) {
          const isSrcColumnNullable = helper.isColumnNullable(sourceTable, relColumn.srcColumnId);
          const isTgtColumnNullable = helper.isColumnNullable(targetTable, relColumn.tgtColumnId);

          if (isSrcColumnNullable || isTgtColumnNullable) {
            // For now, we'll allow the change but could throw an error here
            // throw new Error('Cannot change to 1:1 relationship when FK columns are nullable');
          }
        }
      }
    }

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === sourceTableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
                      relationships: t.relationships.map((r) => (r.id === relationshipId ? { ...r, cardinality } : r)),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  addColumnToRelationship: (database, schemaId, relationshipId, relationshipColumn) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    // Find the relationship in any table of the schema
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
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === sourceTableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
                      relationships: t.relationships.map((r) =>
                        r.id === relationshipId
                          ? {
                              ...r,
                              columns: [...r.columns, { ...relationshipColumn, relationshipId }],
                            }
                          : r
                      ),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  removeColumnFromRelationship: (database, schemaId, relationshipId, relationshipColumnId) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    // Find the relationship in any table of the schema
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
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === sourceTableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
                      // 관계에서 컬럼 제거 후, 빈 관계는 삭제
                      relationships: t.relationships
                        .map((r) =>
                          r.id === relationshipId
                            ? {
                                ...r,
                                columns: r.columns.filter((rc) => rc.id !== relationshipColumnId),
                              }
                            : r
                        )
                        .filter((r) => r.columns.length > 0),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
};
