import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import type { ValidateResult } from '../common';

@Controller()
export class ConstraintsController {
  @GrpcMethod('ValidationService', 'CreateConstraint')
  createConstraint(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'DeleteConstraint')
  deleteConstraint(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'ChangeConstraintName')
  changeConstraintName(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'AddColumnToConstraint')
  addColumnToConstraint(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }

  @GrpcMethod('ValidationService', 'RemoveColumnFromConstraint')
  removeColumnFromConstraint(): ValidateResult {
    return {
      failure: {
        errors: [{ code: 'UNIMPLEMENTED', message: 'Not implemented' }],
      },
    };
  }
}
