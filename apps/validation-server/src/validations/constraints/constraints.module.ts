import { Module } from '@nestjs/common';
import { ConstraintsController } from './constraints.controller';
import { ConstraintsService } from './constraints.service';

@Module({
  controllers: [ConstraintsController],
  providers: [ConstraintsService],
})
export class ConstraintsModule {}
