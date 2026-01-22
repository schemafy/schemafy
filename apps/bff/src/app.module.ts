import { Module } from '@nestjs/common';

import { HealthController } from './health/health.controller.js';
import { MemoModule } from './memo/memo.module.js';

@Module({
  imports: [MemoModule],
  controllers: [HealthController],
})
export class AppModule {}
