import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import type { ValidateResult } from '../common';

@Controller()
export class IndexesController {
  @GrpcMethod('ValidationService', 'CreateIndex')
  createIndex(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'DeleteIndex')
  deleteIndex(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'ChangeIndexName')
  changeIndexName(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'AddColumnToIndex')
  addColumnToIndex(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'RemoveColumnFromIndex')
  removeColumnFromIndex(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }
}
