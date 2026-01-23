import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';

import { CollaborationModule } from './collaboration/collaboration.module';
import { HealthController } from './health/health.controller.js';
import { MemoModule } from './memo/memo.module.js';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    CollaborationModule,
    MemoModule,
  ],
  controllers: [HealthController],
})
export class AppModule {}
