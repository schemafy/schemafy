import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';

import { BackendClientModule } from './common/backend-client/backend-client.module';
import { CollaborationModule } from './collaboration/collaboration.module';
import { ErdModule } from './erd/erd.module';
import { HealthController } from './health/health.controller.js';
import { MemoModule } from './memo/memo.module.js';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    BackendClientModule,
    CollaborationModule,
    ErdModule,
    MemoModule,
  ],
  controllers: [HealthController],
})
export class AppModule {}
