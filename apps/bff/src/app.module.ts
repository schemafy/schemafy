import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';

import { CollaborationModule } from './collaboration/collaboration.module';
import { MemoModule } from './memo/memo.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    CollaborationModule,
    MemoModule,
  ],
})
export class AppModule {}
