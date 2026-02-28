import { createHmac, createHash, randomUUID } from 'crypto';
import type { InternalAxiosRequestConfig } from 'axios';

const HMAC_EXCLUDED_PREFIXES = ['/public/api/', '/ws/'];

type RequestBody = string | Buffer | Record<string, unknown> | null | undefined;

function computeBodyHash(body: RequestBody): string {
  const content = normalizeBody(body);
  return createHash('sha256').update(content).digest('hex');
}

function normalizeBody(body: RequestBody): string | Buffer {
  if (body === undefined || body === null) {
    return '';
  }

  if (typeof body === 'string') {
    return body;
  }

  if (Buffer.isBuffer(body)) {
    return body;
  }

  return JSON.stringify(body);
}

function buildCanonicalString(
  method: string,
  path: string,
  timestamp: string,
  nonce: string,
  bodyHash: string,
): string {
  return `${method}\n${path}\n${timestamp}\n${nonce}\n${bodyHash}\n`;
}

function computeHmac(secret: string, data: string): string {
  return createHmac('sha256', secret).update(data, 'utf8').digest('hex');
}

function shouldSign(method: string, path: string): boolean {
  if (method === 'OPTIONS') {
    return false;
  }

  return !HMAC_EXCLUDED_PREFIXES.some((prefix) => path.startsWith(prefix));
}

export function createHmacHeaders(
  secret: string,
  config: InternalAxiosRequestConfig,
): Record<string, string> | null {
  if (!config.method || !config.url || !config.baseURL) {
    return null;
  }

  const method = config.method.toUpperCase();
  const url = new URL(config.url, config.baseURL);
  const path = url.pathname;

  if (!shouldSign(method, path)) {
    return null;
  }

  if (config.params) {
    const params = new URLSearchParams(config.params);
    params.forEach((value, key) => url.searchParams.append(key, value));
  }

  const fullPath = url.pathname + url.search;

  const timestamp = Date.now().toString();
  const nonce = randomUUID();
  const bodyHash = computeBodyHash(config.data);
  const canonical = buildCanonicalString(
    method,
    fullPath,
    timestamp,
    nonce,
    bodyHash,
  );
  const signature = computeHmac(secret, canonical);

  return {
    'X-Hmac-Signature': signature,
    'X-Hmac-Timestamp': timestamp,
    'X-Hmac-Nonce': nonce,
  };
}
