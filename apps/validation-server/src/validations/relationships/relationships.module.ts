import { Module } from '@nestjs/common';
import { RelationshipsController } from './relationships.controller';

@Module({
  controllers: [RelationshipsController],
})
export class RelationshipsModule {}
