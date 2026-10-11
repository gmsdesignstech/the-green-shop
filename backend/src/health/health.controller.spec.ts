import { Logger, ServiceUnavailableException } from '@nestjs/common';
import { HealthController } from './health.controller';

describe('HealthController', () => {
  it('reports a successful database probe', async () => {
    const controller = new HealthController({ query: jest.fn().mockResolvedValue({ rows: [{ '?column?': 1 }] }) } as any);
    await expect(controller.database()).resolves.toEqual({ status: 'ok' });
  });

  it('returns a safe failure category and logs the server-side stack', async () => {
    const error = Object.assign(new Error('password authentication failed for user secret-user'), { code: '28P01' });
    const logger = jest.spyOn(Logger.prototype, 'error').mockImplementation();
    const controller = new HealthController({ query: jest.fn().mockRejectedValue(error) } as any);
    try {
      await controller.database();
      throw new Error('Expected database health to fail');
    } catch (result) {
      expect(result).toBeInstanceOf(ServiceUnavailableException);
      expect((result as ServiceUnavailableException).getResponse()).toEqual({ status: 'error', code: 'DATABASE_AUTHENTICATION_FAILED' });
      expect(logger).toHaveBeenCalledWith('Database health check failed (DATABASE_AUTHENTICATION_FAILED)', error.stack);
    } finally {
      logger.mockRestore();
    }
  });
});
