import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import type { ValidateResult } from '../common';

@Controller()
export class TablesController {
  @GrpcMethod('ValidationService', 'CreateTable')
  createTable(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'DeleteTable')
  deleteTable(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'ChangeTableName')
  changeTableName(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }
}
