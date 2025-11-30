import { Injectable, Logger } from '@nestjs/common';
import { ERD_VALIDATOR } from '@schemafy/validator';

import { toErrorDetails } from '../common/error-mapper';

import type { ValidateResult } from '../common';
import type { Database } from '@schemafy/validator';

@Injectable()
export class ValidationService {
  private readonly logger = new Logger(ValidationService.name);
  validateDatabase(database: Database): ValidateResult {
    try {
      ERD_VALIDATOR.validate(database);
      this.logger.log('ValidateDatabase request successfully validated');
      return { success: { database } };
    } catch (err) {
      this.logger.error('ValidateDatabase request failed', err);
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
