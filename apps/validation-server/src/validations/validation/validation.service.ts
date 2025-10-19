import { Injectable } from '@nestjs/common';
import { ERD_VALIDATOR } from '@schemafy/validator';

import { toErrorDetails } from '../common/error-mapper';

import type { ValidateResult } from '../common';
import type { Database } from '@schemafy/validator';

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
