import { Module } from '@nestjs/common';
import { IndexesController } from './indexes.controller';

@Module({
  controllers: [IndexesController],
})
export class IndexesModule {}
