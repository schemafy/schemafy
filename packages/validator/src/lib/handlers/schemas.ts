import { SchemaNotExistError, SchemaNameInvalidError, SchemaNameNotUniqueError } from '../errors';
import { Database, SCHEMA, Schema } from '../types';

export interface SchemaHandlers {
  changeSchemaName: (database: Database, schemaId: Schema['id'], newName: Schema['name']) => Database;
  createSchema: (database: Database, schema: Omit<Schema, 'createdAt' | 'updatedAt'>) => Database;
  deleteSchema: (database: Database, schemaId: Schema['id']) => Database;
}

export const schemaHandlers: SchemaHandlers = {
  changeSchemaName: (database, schemaId, newName) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const result = SCHEMA.shape.name.safeParse(newName);
    if (!result.success) throw new SchemaNameInvalidError(newName);

    const existingSchema = database.projects.find((schema) => schema.name === newName && schema.id !== schemaId);
    if (existingSchema) throw new SchemaNameNotUniqueError(newName, existingSchema.id);

    return {
      ...database,
      projects: database.projects.map((schema) =>
        schema.id === schemaId ? { ...schema, name: newName, updatedAt: new Date() } : schema
      ),
    };
  },
  createSchema: (database, schema) => {
    const result = SCHEMA.safeParse(schema);
    if (!result.success) throw new SchemaNameInvalidError(schema.name);

    const existingSchema = database.projects.find((s) => s.name === schema.name);
    if (existingSchema) throw new SchemaNameNotUniqueError(schema.name, existingSchema.id);

    return {
      ...database,
      projects: [...database.projects, { ...schema, createdAt: new Date(), updatedAt: new Date() }],
    };
  },
  deleteSchema: (database, schemaId) => {
    const schema = database.projects.find((s) => s.id === schemaId);

    if (!schema) throw new SchemaNotExistError(schemaId);
    if (schema.deletedAt) throw new SchemaNotExistError(schemaId);

    return {
      ...database,
      projects: database.projects.filter((s) => s.id !== schemaId),
    };
  },
};
