import { readFileSync } from 'node:fs';
import type { PoolConfig } from 'pg';

export type DatabaseFailureCode =
  | 'DATABASE_AUTHENTICATION_FAILED'
  | 'DATABASE_CERTIFICATE_FAILED'
  | 'DATABASE_NAME_INVALID'
  | 'DATABASE_HOST_UNREACHABLE'
  | 'DATABASE_CONNECTION_TIMEOUT'
  | 'DATABASE_CONNECTION_RESET'
  | 'DATABASE_CAPACITY_EXCEEDED'
  | 'DATABASE_PERMISSION_DENIED'
  | 'DATABASE_POOLER_IDENTITY_INVALID'
  | 'DATABASE_RESTARTING'
  | 'DATABASE_CONFIGURATION_INVALID'
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

  // node-postgres replaces the explicit ssl object when these URI parameters
  // are present, which would silently discard DATABASE_SSL_CA.
  for (const parameter of ['sslmode', 'sslcert', 'sslkey', 'sslrootcert']) {
    url.searchParams.delete(parameter);
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
    connectionString: url.toString(),
    application_name: 'the-green-shop-api',
    max: 10,
    idleTimeoutMillis: 30_000,
    connectionTimeoutMillis: 10_000,
    statement_timeout: 5_000,
    query_timeout: 6_000,
    ssl: sslEnabled ? { rejectUnauthorized: true, ...(ca ? { ca } : {}) } : undefined,
  };
}

export function classifyDatabaseFailure(error: unknown): DatabaseFailureCode {
  const candidate = error as { code?: unknown; message?: unknown };
  const code = String(candidate?.code ?? '').toUpperCase();
  const message = String(candidate?.message ?? '').toLowerCase();
  if (code === '28P01' || code === '28000' || message.includes('password authentication failed')) return 'DATABASE_AUTHENTICATION_FAILED';
  if (message.includes('tenant or user not found') || message.includes('invalid tenant')) return 'DATABASE_POOLER_IDENTITY_INVALID';
  if (message.includes('client password must be a string') || message.includes('sasl') && message.includes('password')) return 'DATABASE_CONFIGURATION_INVALID';
  if (code === '3D000' || message.includes('database') && message.includes('does not exist')) return 'DATABASE_NAME_INVALID';
  if (['SELF_SIGNED_CERT_IN_CHAIN', 'UNABLE_TO_VERIFY_LEAF_SIGNATURE', 'DEPTH_ZERO_SELF_SIGNED_CERT', 'CERT_HAS_EXPIRED'].includes(code)
    || message.includes('certificate')) return 'DATABASE_CERTIFICATE_FAILED';
  if (code === 'ENOTFOUND' || code === 'EAI_AGAIN' || code === 'ECONNREFUSED') return 'DATABASE_HOST_UNREACHABLE';
  if (code === 'ETIMEDOUT' || message.includes('connection timeout')) return 'DATABASE_CONNECTION_TIMEOUT';
  if (code === 'ECONNRESET' || message.includes('connection terminated unexpectedly') || message.includes('server closed the connection')) return 'DATABASE_CONNECTION_RESET';
  if (code === '53300' || message.includes('too many connections')) return 'DATABASE_CAPACITY_EXCEEDED';
  if (code === '42501' || message.includes('permission denied')) return 'DATABASE_PERMISSION_DENIED';
  if (code === '57P01' || code === '57P02' || code === '57P03') return 'DATABASE_RESTARTING';
  return 'DATABASE_UNAVAILABLE';
}
