import { Module } from '@nestjs/common';
import { APP_FILTER } from '@nestjs/core';
import { ConfigModule } from '@nestjs/config';

import { BackendClientModule } from './common/backend-client/backend-client.module';
import { CollaborationModule } from './collaboration/collaboration.module';
import { ErdModule } from './erd/erd.module';
import { HealthController } from './health/health.controller.js';
import { MemoModule } from './memo/memo.module.js';
import { AuthModule } from './auth/auth.module';
import { HttpExceptionFilter } from './common/filters/http-exception.filter.js';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    BackendClientModule,
    CollaborationModule,
    ErdModule,
    MemoModule,
    AuthModule,
  ],
  controllers: [HealthController],
  providers: [
    {
      provide: APP_FILTER,
      useClass: HttpExceptionFilter,
    },
  ],
})
export class AppModule {}
