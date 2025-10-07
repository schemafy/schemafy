import { Module } from '@nestjs/common';
import { TablesController } from './tables.controller';

@Module({
  controllers: [TablesController],
})
export class TablesModule {}
