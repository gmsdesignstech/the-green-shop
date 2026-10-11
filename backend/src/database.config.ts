import { readFileSync } from 'node:fs';
import type { PoolConfig } from 'pg';

export type DatabaseFailureCode =
  | 'DATABASE_AUTHENTICATION_FAILED'
  | 'DATABASE_CERTIFICATE_FAILED'
  | 'DATABASE_NAME_INVALID'
  | 'DATABASE_HOST_UNREACHABLE'
  | 'DATABASE_CONNECTION_TIMEOUT'
  | 'DATABASE_UNAVAILABLE';

export function databasePoolConfig(env: NodeJS.ProcessEnv = process.env): PoolConfig {
  const connectionString = env.DATABASE_URL?.trim();
  if (!connectionString) throw new Error('DATABASE_URL is required');

  let url: URL;
  try {
    url = new URL(connectionString);
  } catch {
    throw new Error('DATABASE_URL must be a valid PostgreSQL URL');
  }
  if (!['postgres:', 'postgresql:'].includes(url.protocol) || !url.hostname || !url.username || !url.password || url.pathname === '/') {
    throw new Error('DATABASE_URL must include a PostgreSQL host, database, username and password');
  }

  const isProduction = env.NODE_ENV === 'production';
  if (isProduction && env.DATABASE_SSL === 'false') {
    throw new Error('DATABASE_SSL cannot be disabled in production');
  }
  const sslEnabled = isProduction || env.DATABASE_SSL === 'true';
  const inlineCa = env.DATABASE_SSL_CA?.replace(/\\n/g, '\n').trim();
  const caPath = env.DATABASE_SSL_CA_PATH?.trim();
  const ca = inlineCa || (caPath ? readFileSync(caPath, 'utf8') : undefined);

  return {
    connectionString,
    application_name: 'the-green-shop-api',
    max: 10,
    idleTimeoutMillis: 30_000,
    connectionTimeoutMillis: 10_000,
    ssl: sslEnabled ? { rejectUnauthorized: true, ...(ca ? { ca } : {}) } : undefined,
  };
}

export function classifyDatabaseFailure(error: unknown): DatabaseFailureCode {
  const candidate = error as { code?: unknown; message?: unknown };
  const code = String(candidate?.code ?? '').toUpperCase();
  const message = String(candidate?.message ?? '').toLowerCase();
  if (code === '28P01' || code === '28000' || message.includes('password authentication failed')) return 'DATABASE_AUTHENTICATION_FAILED';
  if (code === '3D000' || message.includes('database') && message.includes('does not exist')) return 'DATABASE_NAME_INVALID';
  if (['SELF_SIGNED_CERT_IN_CHAIN', 'UNABLE_TO_VERIFY_LEAF_SIGNATURE', 'DEPTH_ZERO_SELF_SIGNED_CERT', 'CERT_HAS_EXPIRED'].includes(code)
    || message.includes('certificate')) return 'DATABASE_CERTIFICATE_FAILED';
  if (code === 'ENOTFOUND' || code === 'EAI_AGAIN' || code === 'ECONNREFUSED') return 'DATABASE_HOST_UNREACHABLE';
  if (code === 'ETIMEDOUT' || message.includes('connection timeout')) return 'DATABASE_CONNECTION_TIMEOUT';
  return 'DATABASE_UNAVAILABLE';
}
