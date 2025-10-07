import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import type { ValidateResult } from '../common';

@Controller()
export class SchemasController {
  @GrpcMethod('ValidationService', 'CreateSchema')
  createSchema(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'DeleteSchema')
  deleteSchema(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'ChangeSchemaName')
  changeSchemaName(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }
}
