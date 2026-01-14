import { Module } from '@nestjs/common';

import { AppController } from './app.controller';
import { AppService } from './app.service';
import { MemoModule } from './memo/memo.module';

@Module({
  imports: [MemoModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
