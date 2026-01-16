import { Module } from '@nestjs/common';

import { AppController } from './app.controller';
import { AppService } from './app.service';
import { CollaborationModule } from './collaboration/collaboration.module';

@Module({
  imports: [CollaborationModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
