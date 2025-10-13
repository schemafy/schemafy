import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { ValidationService } from './validation.service';

import type { ValidateDatabaseDto } from './dto';
import type { ValidateResult } from '../common';

@Controller()
export class ValidationController {
    constructor(private readonly service: ValidationService) {}

    @GrpcMethod('ValidationService', 'ValidateDatabase')
    validateDatabase(req: ValidateDatabaseDto): ValidateResult {
        return this.service.validateDatabase(req.database);
    }
}
