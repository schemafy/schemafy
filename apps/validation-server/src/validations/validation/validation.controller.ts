import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { ValidationService } from './validation.service';

import type { ValidateResult } from '../common';
import type { ValidateDatabaseRequest } from '../../types/validation.types';

@Controller()
export class ValidationController {
  constructor(private readonly service: ValidationService) {}

  @GrpcMethod('ValidationService', 'ValidateDatabase')
  validateDatabase(req: ValidateDatabaseRequest): ValidateResult {
    return this.service.validateDatabase(req.database);
  }
}
