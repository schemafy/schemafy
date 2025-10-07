import { Injectable } from '@nestjs/common';
import type { ValidateResult } from '../common';
import { toErrorDetails } from '../common/error-mapper';
import type { Database } from '@schemafy/validator';
import { ERD_VALIDATOR } from '@schemafy/validator';

@Injectable()
export class ValidationService {
  validateDatabase(database: Database): ValidateResult {
    try {
      ERD_VALIDATOR.validate(database);
      return { success: { database } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
