import { NestFactory } from '@nestjs/core';
import { WsAdapter } from '@nestjs/platform-ws';
import { Logger } from '@nestjs/common';

import { AppModule } from './app.module.js';
import { LoggingInterceptor } from './common/interceptors/logging.interceptor.js';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  app.useWebSocketAdapter(new WsAdapter(app));
  const logger = new Logger('Bootstrap');

  app.enableCors({
    origin: process.env.FRONTEND_URL || 'http://localhost:3001',
    credentials: true,
    allowedHeaders: [
      'Content-Type',
      'Accept',
      'Authorization',
      'X-Session-Id',
      'X-Client-Op-Id',
      'X-Base-Schema-Revision',
    ],
  });

  app.useGlobalInterceptors(new LoggingInterceptor());

  const port = Number(process.env.PORT) || 4000;
  await app.listen(port, '0.0.0.0');
  logger.log(`BFF is running on port ${port}`);
}

void bootstrap();
