import { Injectable, Logger } from '@nestjs/common';
import { ERD_VALIDATOR } from '@schemafy/validator';

import { toErrorDetails } from '../common/error-mapper';

import type { ValidateResult } from '../common';
import type { Database, Schema } from '@schemafy/validator';

@Injectable()
export class SchemasService {
  private readonly logger = new Logger(SchemasService.name);
  changeSchemaName(database: Database, schemaId: Schema['id'], newName: Schema['name']): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeSchemaName(database, schemaId, newName);
      this.logger.log(`ChangeSchemaName request successfully validated, schemaId: ${schemaId}, newName: ${newName}`);
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(`ChangeSchemaName request failed, schemaId: ${schemaId}, newName: ${newName}`, err);
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  createSchema(database: Database, schema: Omit<Schema, 'createdAt' | 'updatedAt'>): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.createSchema(database, schema);
      this.logger.log(`CreateSchema request successfully validated, schema: ${JSON.stringify(schema)}`);
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(`CreateSchema request failed, schema: ${JSON.stringify(schema)}`, err);
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  deleteSchema(database: Database, schemaId: Schema['id']): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.deleteSchema(database, schemaId);
      this.logger.log(`DeleteSchema request successfully validated, schemaId: ${schemaId}`);
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(`DeleteSchema request failed, schemaId: ${schemaId}`, err);
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
