import { createHmac, createHash, randomUUID } from 'crypto';
import type { InternalAxiosRequestConfig } from 'axios';

const HMAC_EXCLUDED_PREFIXES = ['/public/api/', '/ws/'];

function computeBodyHash(body: unknown): string {
  const hash = createHash('sha256');

  if (body === undefined || body === null) {
    hash.update('');
  } else if (typeof body === 'string') {
    hash.update(body, 'utf8');
  } else if (Buffer.isBuffer(body)) {
    hash.update(body);
  } else {
    hash.update(JSON.stringify(body), 'utf8');
  }

  return hash.digest('hex');
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
  const method = (config.method ?? 'GET').toUpperCase();
  const url = new URL(config.url ?? '/', config.baseURL ?? 'http://localhost');
  const path = url.pathname;

  if (!shouldSign(method, path)) {
    return null;
  }

  const query = url.search ? url.search.slice(1) : '';
  const fullPath = query ? `${path}?${query}` : path;

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
