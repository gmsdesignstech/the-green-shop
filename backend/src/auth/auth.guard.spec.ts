import { ExecutionContext, UnauthorizedException } from '@nestjs/common';
import { decodeJwt, jwtVerify } from 'jose';
import { AuthGuard } from './auth.guard';

jest.mock('jose', () => ({
  createRemoteJWKSet: jest.fn(() => jest.fn()),
  jwtVerify: jest.fn(),
  decodeJwt: jest.fn(),
}));

describe('AuthGuard', () => {
  const devAuth = { verifyAccessToken: jest.fn().mockResolvedValue(null) } as any;
  const request = { headers: { authorization: 'Bearer test-token' } } as any;
  const context = {
    switchToHttp: () => ({ getRequest: () => request }),
  } as unknown as ExecutionContext;

  beforeEach(() => {
    jest.resetAllMocks();
    process.env.SUPABASE_URL = 'https://project.supabase.co';
    process.env.SUPABASE_PUBLISHABLE_KEY = 'publishable-key';
    request.user = undefined;
    devAuth.verifyAccessToken.mockResolvedValue(null);
  });

  it('accepts a locally verified asymmetric token', async () => {
    (jwtVerify as jest.Mock).mockResolvedValue({ payload: { sub: 'admin-1' } });

    await expect(new AuthGuard(devAuth).canActivate(context)).resolves.toBe(true);
    expect(request.user).toEqual({ id: 'admin-1' });
  });

  it('validates shared-secret tokens with Supabase Auth', async () => {
    (jwtVerify as jest.Mock).mockRejectedValue(new Error('No matching JWKS key'));
    (decodeJwt as jest.Mock).mockReturnValue({
      iss: 'https://project.supabase.co/auth/v1', aud: 'authenticated', sub: 'partner-1',
    });
    global.fetch = jest.fn().mockResolvedValue({ ok: true, json: async () => ({ id: 'partner-1' }) }) as any;

    await expect(new AuthGuard(devAuth).canActivate(context)).resolves.toBe(true);
    expect(request.user).toEqual({ id: 'partner-1' });
    expect(global.fetch).toHaveBeenCalledWith(
      'https://project.supabase.co/auth/v1/user',
      expect.objectContaining({ headers: expect.objectContaining({ apikey: 'publishable-key' }) }),
    );
  });

  it('rejects tokens rejected by both verification paths', async () => {
    (jwtVerify as jest.Mock).mockRejectedValue(new Error('Invalid token'));
    global.fetch = jest.fn().mockResolvedValue({ ok: false }) as any;

    await expect(new AuthGuard(devAuth).canActivate(context)).rejects.toBeInstanceOf(UnauthorizedException);
    expect(request.user).toBeUndefined();
  });
});
