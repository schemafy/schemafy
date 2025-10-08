import { Injectable } from '@nestjs/common';
import { ERD_VALIDATOR } from '@schemafy/validator';

import { toErrorDetails } from '../common/error-mapper';

import type { ValidateResult } from '../common';
import type { Database, Schema } from '@schemafy/validator';

@Injectable()
export class SchemasService {
  changeSchemaName(
    database: Database,
    schemaId: Schema['id'],
    newName: Schema['name'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeSchemaName(
        database,
        schemaId,
        newName,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  createSchema(
    database: Database,
    schema: Omit<Schema, 'createdAt' | 'updatedAt'>,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.createSchema(database, schema);
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  deleteSchema(database: Database, schemaId: Schema['id']): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.deleteSchema(database, schemaId);
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
