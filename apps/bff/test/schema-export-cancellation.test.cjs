require('reflect-metadata');
const { test } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { once } = require('node:events');
const { Module } = require('@nestjs/common');
const { NestFactory } = require('@nestjs/core');
const { SchemaController } = require('../dist/erd/schema.controller');
const { SchemaService } = require('../dist/erd/schema.service');
const {
  HttpExceptionFilter,
} = require('../dist/common/filters/http-exception.filter');
const {
  LoggingInterceptor,
} = require('../dist/common/interceptors/logging.interceptor');
const {
  BackendClientService,
} = require('../dist/common/backend-client/backend-client.service');

test('export proxies responses and closes the backend request when the client disconnects', async (t) => {
  let backendRequest;
  let requestReceived;
  const backend = http.createServer((req, res) => {
    backendRequest = { req, res };
    requestReceived?.();
    if (req.url.includes('success')) {
      res.setHeader('content-type', 'application/json');
      res.end(JSON.stringify({ schemaId: 'success', mermaid: 'erDiagram' }));
    } else if (req.url.includes('failure')) {
      res.writeHead(503);
      res.end();
    }
  });
  backend.listen(0, '127.0.0.1');
  await once(backend, 'listening');
  t.after(() => new Promise((resolve) => backend.close(resolve)));
  const backendUrl = `http://127.0.0.1:${backend.address().port}`;
  const config = {
    get: (key, fallback) => (key === 'BACKEND_URL' ? backendUrl : fallback),
  };
  const client = new BackendClientService(config);
  class TestModule {}
  Module({
    controllers: [SchemaController],
    providers: [
      SchemaService,
      { provide: BackendClientService, useValue: client },
    ],
  })(TestModule);
  const logs = [];
  const logger = {
    log() {},
    debug() {},
    verbose() {},
    fatal() {},
    warn: (message) => logs.push(String(message)),
    error: (message) => logs.push(String(message)),
  };
  const app = await NestFactory.create(TestModule, { logger });
  app.useGlobalFilters(new HttpExceptionFilter(config));
  app.useGlobalInterceptors(new LoggingInterceptor());
  await app.listen(0, '127.0.0.1');
  t.after(() => app.close());
  const base = await app.getUrl();
  const headers = { authorization: 'Bearer test' };

  const success = await fetch(
    `${base}/api/v1.0/schemas/success/exports/mermaid`,
    { headers },
  );
  assert.equal(success.status, 200);
  assert.deepEqual(await success.json(), {
    schemaId: 'success',
    mermaid: 'erDiagram',
  });
  const failure = await fetch(
    `${base}/api/v1.0/schemas/failure/exports/mermaid`,
    { headers },
  );
  assert.equal(failure.status, 503);
  assert.ok(logs.some((message) => message.includes('Backend error: 503')));

  for (const format of ['ddl?targetDbVendor=mysql', 'mermaid']) {
    const received = new Promise((resolve) => {
      requestReceived = resolve;
    });
    const controller = new AbortController();
    const request = fetch(
      `${base}/api/v1.0/schemas/pending/exports/${format}`,
      { headers, signal: controller.signal },
    );
    const rejected = assert.rejects(request, { name: 'AbortError' });
    await received;
    const closed = once(backendRequest.res, 'close');
    controller.abort();
    await rejected;
    await Promise.race([
      closed,
      new Promise((_, reject) =>
        setTimeout(
          () => reject(new Error('backend request was not cancelled')),
          2000,
        ),
      ),
    ]);
    assert.equal(backendRequest.res.writableFinished, false);
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(
      logs.some((message) => message.includes('/schemas/pending/')),
      false,
    );
  }
});
