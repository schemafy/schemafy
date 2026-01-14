import { Module } from '@nestjs/common';

import { MemoModule } from './memo/memo.module';

@Module({
  imports: [MemoModule],
})
export class AppModule {}
