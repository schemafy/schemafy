import {
  SchemaNotExistError,
  SchemaNameInvalidError,
  SchemaNameNotUniqueError,
  DatabaseEmptySchemaError,
} from '../errors';
import { SCHEMA_NAME_CONSTRAINTS } from '@/types/erd.types';
import type { Database, Schema } from '@/types/erd.types';

const isValidSchemaName = (name: string) => {
  return (
    name.length >= SCHEMA_NAME_CONSTRAINTS.MIN_LENGTH &&
    name.length <= SCHEMA_NAME_CONSTRAINTS.MAX_LENGTH
  );
};

export interface SchemaHandlers {
  changeSchemaName: (
    database: Database,
    schemaId: Schema['id'],
    newName: Schema['name'],
  ) => Database;
  createSchema: (database: Database, schema: Schema) => Database;
  deleteSchema: (database: Database, schemaId: Schema['id']) => Database;
}

export const schemaHandlers: SchemaHandlers = {
  changeSchemaName: (database, schemaId, newName) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    if (!isValidSchemaName(newName)) throw new SchemaNameInvalidError(newName);

    const existingSchema = database.schemas.find(
      (schema) => schema.name === newName && schema.id !== schemaId,
    );
    if (existingSchema)
      throw new SchemaNameNotUniqueError(newName, existingSchema.id);

    return {
      ...database,
      isAffected: true,
      schemas: database.schemas.map((schema) =>
        schema.id === schemaId
          ? { ...schema, name: newName, isAffected: true }
          : schema,
      ),
    };
  },
  createSchema: (database, schema) => {
    if (!isValidSchemaName(schema.name))
      throw new SchemaNameInvalidError(schema.name);

    const existingSchema = database.schemas.find((s) => s.name === schema.name);
    if (existingSchema)
      throw new SchemaNameNotUniqueError(schema.name, existingSchema.id);

    return {
      ...database,
      isAffected: true,
      schemas: [
        ...database.schemas,
        {
          ...schema,
          isAffected: true,
        },
      ],
    };
  },
  deleteSchema: (database, schemaId) => {
    if (database.schemas.length === 1) throw new DatabaseEmptySchemaError();

    const schema = database.schemas.find((s) => s.id === schemaId);

    if (!schema) throw new SchemaNotExistError(schemaId);

    return {
      ...database,
      isAffected: true,
      schemas: database.schemas.filter((s) => s.id !== schemaId),
    };
  },
};
