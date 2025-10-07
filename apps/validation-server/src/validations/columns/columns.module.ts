import { Module } from '@nestjs/common';
import { ColumnsController } from './columns.controller';

@Module({
  controllers: [ColumnsController],
})
export class ColumnsModule {}
