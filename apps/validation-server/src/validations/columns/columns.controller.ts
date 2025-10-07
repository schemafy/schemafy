import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import type { ValidateResult } from '../common';

@Controller()
export class ColumnsController {
  @GrpcMethod('ValidationService', 'CreateColumn')
  createColumn(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'DeleteColumn')
  deleteColumn(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'ChangeColumnName')
  changeColumnName(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'ChangeColumnType')
  changeColumnType(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'ChangeColumnPosition')
  changeColumnPosition(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'ChangeColumnNullable')
  changeColumnNullable(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }
}
