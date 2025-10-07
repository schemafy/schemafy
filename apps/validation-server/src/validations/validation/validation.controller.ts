import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import type { ValidateResult } from '../common';
import { ValidationService } from './validation.service';
import type { Database } from '@schemafy/validator';

@Controller()
export class ValidationController {
  constructor(private readonly service: ValidationService) {}

  @GrpcMethod('ValidationService', 'ValidateDatabase')
  validateDatabase(database: Database): ValidateResult {
    return this.service.validateDatabase(database);
  }
}
