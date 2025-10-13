import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { ConstraintsService } from './constraints.service';

import type {
    AddColumnToConstraintDto,
    ChangeConstraintNameDto,
    CreateConstraintDto,
    DeleteConstraintDto,
    RemoveColumnFromConstraintDto,
} from './dto';
import type { ValidateResult } from '../common';

@Controller()
export class ConstraintsController {
    constructor(private readonly service: ConstraintsService) {}

    @GrpcMethod('ValidationService', 'CreateConstraint')
    createConstraint(req: CreateConstraintDto): ValidateResult {
        const { database, schemaId, tableId, constraint } = req;
        return this.service.createConstraint(database, schemaId, tableId, constraint);
    }

    @GrpcMethod('ValidationService', 'DeleteConstraint')
    deleteConstraint(req: DeleteConstraintDto): ValidateResult {
        const { database, schemaId, tableId, constraintId } = req;
        return this.service.deleteConstraint(database, schemaId, tableId, constraintId);
    }

    @GrpcMethod('ValidationService', 'ChangeConstraintName')
    changeConstraintName(req: ChangeConstraintNameDto): ValidateResult {
        const { database, schemaId, tableId, constraintId, newName } = req;
        return this.service.changeConstraintName(database, schemaId, tableId, constraintId, newName);
    }

    @GrpcMethod('ValidationService', 'AddColumnToConstraint')
    addColumnToConstraint(req: AddColumnToConstraintDto): ValidateResult {
        const { database, schemaId, tableId, constraintId, constraintColumn } = req;
        return this.service.addColumnToConstraint(database, schemaId, tableId, constraintId, constraintColumn);
    }

    @GrpcMethod('ValidationService', 'RemoveColumnFromConstraint')
    removeColumnFromConstraint(req: RemoveColumnFromConstraintDto): ValidateResult {
        const { database, schemaId, tableId, constraintId, constraintColumnId } = req;
        return this.service.removeColumnFromConstraint(database, schemaId, tableId, constraintId, constraintColumnId);
    }
}
