import { Injectable } from '@nestjs/common';
import { ERD_VALIDATOR } from '@schemafy/validator';

import { toErrorDetails } from '../common/error-mapper';

import type { ValidateResult } from '../common';
import type { Database, Table, Schema } from '@schemafy/validator';

@Injectable()
export class TablesService {
    createTable(
        database: Database,
        schemaId: Schema['id'],
        table: Omit<Table, 'schemaId' | 'createdAt' | 'updatedAt'>,
    ): ValidateResult {
        try {
            const updated = ERD_VALIDATOR.createTable(database, schemaId, table);
            return { success: { database: updated } };
        } catch (err) {
            return { failure: { errors: toErrorDetails(err) } };
        }
    }

    deleteTable(database: Database, schemaId: Schema['id'], tableId: Table['id']): ValidateResult {
        try {
            const updated = ERD_VALIDATOR.deleteTable(database, schemaId, tableId);
            return { success: { database: updated } };
        } catch (err) {
            return { failure: { errors: toErrorDetails(err) } };
        }
    }

    changeTableName(
        database: Database,
        schemaId: Schema['id'],
        tableId: Table['id'],
        newName: Table['name'],
    ): ValidateResult {
        try {
            const updated = ERD_VALIDATOR.changeTableName(database, schemaId, tableId, newName);
            return { success: { database: updated } };
        } catch (err) {
            return { failure: { errors: toErrorDetails(err) } };
        }
    }
}
