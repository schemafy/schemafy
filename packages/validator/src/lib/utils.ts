import { SchemaNotExistError, SchemaNameInvalidError, SchemaNameNotUniqueError } from './errors';
import { Database, SCHEMA, Schema } from './types';

interface ERDValidator {
  changeSchemaName: (database: Database, schemaId: Schema['id'], newName: Schema['name']) => Database;
}

export const ERD_VALIDATOR: ERDValidator = (() => {
  return {
    changeSchemaName: (database, schemaId, newName) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const result = SCHEMA.shape.name.safeParse(newName);
      if (!result.success) throw new SchemaNameInvalidError(newName);

      const existingSchema = database.projects.find((schema) => schema.name === newName && schema.id !== schemaId);
      if (existingSchema) throw new SchemaNameNotUniqueError(newName, existingSchema.id);

      return {
        ...database,
        projects: database.projects.map((schema) => (schema.id === schemaId ? { ...schema, name: newName } : schema)),
      };
    },
  };
})();
