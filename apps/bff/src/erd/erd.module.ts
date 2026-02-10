import { Module } from '@nestjs/common';
import { ErdController } from './erd.controller';
import { ErdService } from './erd.service';

@Module({
  controllers: [ErdController],
  providers: [ErdService],
})
export class ErdModule {}
