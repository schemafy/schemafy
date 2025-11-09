import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { join } from 'node:path';

import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  app.connectMicroservice<MicroserviceOptions>({
    transport: Transport.GRPC,
    options: {
      package: 'validation',
      protoPath: [join(__dirname, '..', 'protos', 'validation.proto')],
      url: process.env.GRPC_URL ?? '0.0.0.0:50051',
      loader: {
        enums: String,
        arrays: true,
      },
    },
  });

  await app.startAllMicroservices();
  await app.listen(process.env.PORT ?? 3000);
}

void bootstrap();
