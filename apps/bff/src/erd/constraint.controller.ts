import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  Param,
  Patch,
  Post,
} from '@nestjs/common';
import { ConstraintService } from './constraint.service';
import type {
  AddConstraintColumnRequest,
  ChangeConstraintCheckExprRequest,
  ChangeConstraintColumnPositionRequest,
  ChangeConstraintDefaultExprRequest,
  ChangeConstraintNameRequest,
  CreateConstraintRequest,
} from './erd.types';

@Controller('api/v1.0')
export class ConstraintController {
  constructor(private readonly constraintService: ConstraintService) {}

  @Post('constraints')
  async createConstraint(
    @Body() data: CreateConstraintRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.createConstraint(data, authHeader);
  }

  @Get('constraints/:constraintId')
  async getConstraint(
    @Param('constraintId') constraintId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.getConstraint(constraintId, authHeader);
  }

  @Get('tables/:tableId/constraints')
  async getConstraintsByTableId(
    @Param('tableId') tableId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.getConstraintsByTableId(tableId, authHeader);
  }

  @Patch('constraints/:constraintId/name')
  async changeConstraintName(
    @Param('constraintId') constraintId: string,
    @Body() data: ChangeConstraintNameRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.changeConstraintName(
      constraintId,
      data,
      authHeader,
    );
  }

  @Patch('constraints/:constraintId/check-expr')
  async changeConstraintCheckExpr(
    @Param('constraintId') constraintId: string,
    @Body() data: ChangeConstraintCheckExprRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.changeConstraintCheckExpr(
      constraintId,
      data,
      authHeader,
    );
  }

  @Patch('constraints/:constraintId/default-expr')
  async changeConstraintDefaultExpr(
    @Param('constraintId') constraintId: string,
    @Body() data: ChangeConstraintDefaultExprRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.changeConstraintDefaultExpr(
      constraintId,
      data,
      authHeader,
    );
  }

  @Delete('constraints/:constraintId')
  async deleteConstraint(
    @Param('constraintId') constraintId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.deleteConstraint(constraintId, authHeader);
  }

  @Get('constraints/:constraintId/columns')
  async getConstraintColumns(
    @Param('constraintId') constraintId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.getConstraintColumns(
      constraintId,
      authHeader,
    );
  }

  @Post('constraints/:constraintId/columns')
  async addConstraintColumn(
    @Param('constraintId') constraintId: string,
    @Body() data: AddConstraintColumnRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.addConstraintColumn(
      constraintId,
      data,
      authHeader,
    );
  }

  @Delete('constraint-columns/:constraintColumnId')
  async removeConstraintColumn(
    @Param('constraintColumnId') constraintColumnId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.removeConstraintColumn(
      constraintColumnId,
      authHeader,
    );
  }

  @Get('constraint-columns/:constraintColumnId')
  async getConstraintColumn(
    @Param('constraintColumnId') constraintColumnId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.getConstraintColumn(
      constraintColumnId,
      authHeader,
    );
  }

  @Patch('constraint-columns/:constraintColumnId/position')
  async changeConstraintColumnPosition(
    @Param('constraintColumnId') constraintColumnId: string,
    @Body() data: ChangeConstraintColumnPositionRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.constraintService.changeConstraintColumnPosition(
      constraintColumnId,
      data,
      authHeader,
    );
  }
}
