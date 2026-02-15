import { Global, Module } from '@nestjs/common';
import { BackendClientService } from './backend-client.service';

@Global()
@Module({
  providers: [BackendClientService],
  exports: [BackendClientService],
})
export class BackendClientModule {}
