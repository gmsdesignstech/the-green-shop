import { classifyDatabaseFailure, databasePoolConfig } from './database.config';

describe('database configuration', () => {
  it('requires a complete PostgreSQL URL without exposing its value', () => {
    expect(() => databasePoolConfig({ NODE_ENV: 'production', DATABASE_SSL: 'true', DATABASE_URL: 'not-a-url' }))
      .toThrow('DATABASE_URL must be a valid PostgreSQL URL');
    expect(() => databasePoolConfig({ NODE_ENV: 'production', DATABASE_SSL: 'true', DATABASE_URL: 'postgresql://host/postgres' }))
      .toThrow('DATABASE_URL must include a PostgreSQL host, database, username and password');
  });

  it('defaults to verified TLS in production, rejects opt-out and accepts a multiline hosted CA', () => {
    const url = 'postgresql://user:password@db.example.com:5432/postgres';
    expect(databasePoolConfig({ NODE_ENV: 'production', DATABASE_URL: url }).ssl)
      .toEqual({ rejectUnauthorized: true });
    expect(() => databasePoolConfig({ NODE_ENV: 'production', DATABASE_SSL: 'false', DATABASE_URL: url }))
      .toThrow('DATABASE_SSL cannot be disabled in production');
    const config = databasePoolConfig({
      NODE_ENV: 'production', DATABASE_SSL: 'true', DATABASE_URL: url,
      DATABASE_SSL_CA: '-----BEGIN CERTIFICATE-----\\nexample\\n-----END CERTIFICATE-----',
    });
    expect(config.ssl).toEqual(expect.objectContaining({ rejectUnauthorized: true }));
    expect((config.ssl as { ca: string }).ca).toContain('\nexample\n');
  });

  it.each([
    [{ code: '28P01' }, 'DATABASE_AUTHENTICATION_FAILED'],
    [{ code: 'SELF_SIGNED_CERT_IN_CHAIN' }, 'DATABASE_CERTIFICATE_FAILED'],
    [{ code: 'ENOTFOUND' }, 'DATABASE_HOST_UNREACHABLE'],
    [{ code: 'ETIMEDOUT' }, 'DATABASE_CONNECTION_TIMEOUT'],
    [{ code: '3D000' }, 'DATABASE_NAME_INVALID'],
  ])('classifies failures without returning credentials', (error, expected) => {
    expect(classifyDatabaseFailure(error)).toBe(expected);
  });
});
