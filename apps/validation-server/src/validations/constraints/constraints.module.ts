import { Module } from '@nestjs/common';
import { ConstraintsController } from './constraints.controller';

@Module({
  controllers: [ConstraintsController],
})
export class ConstraintsModule {}
