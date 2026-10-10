import { ExecutionContext, ForbiddenException } from '@nestjs/common';
import { AdminGuard } from './admin.guard';

describe('AdminGuard', () => {
  const context = (id = 'user-1', role?: 'customer'|'admin'|'delivery_partner') => ({
    switchToHttp: () => ({ getRequest: () => ({ user: { id, role } }) }),
  } as unknown as ExecutionContext);

  it('allows an active trusted administrator', async () => {
    const db = { query: jest.fn().mockResolvedValue({ rowCount: 1 }) } as any;
    await expect(new AdminGuard(db).canActivate(context())).resolves.toBe(true);
    expect(db.query).toHaveBeenCalledWith(expect.stringContaining("role='admin'"), ['user-1']);
  });

  it('rejects users without an active admin assignment', async () => {
    const db = { query: jest.fn().mockResolvedValue({ rowCount: 0 }) } as any;
    await expect(new AdminGuard(db).canActivate(context())).rejects.toBeInstanceOf(ForbiddenException);
  });

  it('accepts only an authenticated development admin role without querying production data', async () => {
    const db = { query: jest.fn() } as any;
    await expect(new AdminGuard(db).canActivate(context('dev-admin', 'admin'))).resolves.toBe(true);
    await expect(new AdminGuard(db).canActivate(context('dev-customer', 'customer'))).rejects.toBeInstanceOf(ForbiddenException);
    expect(db.query).not.toHaveBeenCalled();
  });
});
