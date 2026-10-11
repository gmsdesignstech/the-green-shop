import { ForbiddenException, Injectable, UnauthorizedException } from '@nestjs/common';
import { createHash, randomBytes, randomUUID } from 'node:crypto';
import { SignJWT, jwtVerify } from 'jose';

type LoginRole = 'customer' | 'admin' | 'delivery_partner';
type DevSession = {
  id: string;
  userId: string;
  phone: string;
  role: LoginRole;
  refreshTokenHash: string;
  expiresAt: number;
  revoked: boolean;
};

export const DEVELOPMENT_USER_IDS: Record<LoginRole, string> = {
  customer: '00000000-0000-4000-8000-000000000001',
  admin: '00000000-0000-4000-8000-000000000002',
  delivery_partner: '00000000-0000-4000-8000-000000000003',
};

/** Isolated local authentication. Development sessions never enter production data. */
@Injectable()
export class DevAuthService {
  private readonly enabled = process.env.NODE_ENV === 'development' && process.env.DEV_OTP_ENABLED === 'true';
  private readonly secret = new TextEncoder().encode(process.env.DEV_AUTH_JWT_SECRET ?? '');
  private readonly issuer = 'the-green-shop-dev-auth';
  private readonly sessions = new Map<string, DevSession>();

  constructor() {
    if (process.env.DEV_OTP_ENABLED === 'true' && process.env.NODE_ENV !== 'development') {
      throw new Error('DEV_OTP_ENABLED may only be enabled when NODE_ENV=development');
    }
    if (this.enabled && this.secret.length < 32) throw new Error('DEV_AUTH_JWT_SECRET must be at least 32 characters');
    if (this.enabled) {
      const otp = process.env.DEV_OTP_CODE ?? '';
      if (!/^\d{6}$/.test(otp)) throw new Error('DEV_OTP_CODE must contain exactly 6 digits');
      for (const name of [
        'DEV_CUSTOMER_TEST_PHONE_NUMBER',
        'DEV_ADMIN_TEST_PHONE_NUMBER',
        'DEV_DELIVERY_PARTNER_TEST_PHONE_NUMBER',
      ]) {
        if (!/^\+91[6-9]\d{9}$/.test(process.env[name] ?? '')) {
          throw new Error(`${name} must be a valid Indian E.164 phone number when development OTP is enabled`);
        }
      }
    }
  }

  async login(phone: string, otp: string, role: LoginRole) {
    this.requireEnabled();
    if (phone !== this.allowedPhone(role)) throw new ForbiddenException('This phone number is not authorized for development login');
    if (!this.safeOtpMatch(otp)) throw new UnauthorizedException('Invalid development OTP');
    return this.issueSession(DEVELOPMENT_USER_IDS[role], phone, role);
  }

  async refresh(refreshToken: string) {
    this.requireEnabled();
    const hash = this.hash(refreshToken);
    const session = [...this.sessions.values()].find(item =>
      !item.revoked && item.expiresAt > Date.now() && item.refreshTokenHash === hash);
    if (!session) throw new UnauthorizedException('Development session expired');
    const rotated = randomBytes(32).toString('base64url');
    session.refreshTokenHash = this.hash(rotated);
    return {
      ...(await this.accessToken(session.userId, session.role, session.id)),
      refreshToken: rotated,
      user: { id: session.userId, phone: session.phone, role: session.role },
    };
  }

  async logout(refreshToken: string) {
    if (!this.enabled) return { success: true };
    const hash = this.hash(refreshToken);
    for (const session of this.sessions.values()) {
      if (session.refreshTokenHash === hash) session.revoked = true;
    }
    return { success: true };
  }

  async verifyAccessToken(token: string): Promise<{ id: string; role: LoginRole } | null> {
    if (!this.enabled) return null;
    try {
      const { payload } = await jwtVerify(token, this.secret, { issuer: this.issuer, audience: 'the-green-shop-api' });
      const role = String(payload.role) as LoginRole;
      const session = payload.sid ? this.sessions.get(String(payload.sid)) : undefined;
      if (!payload.sub || !session || session.revoked || session.expiresAt <= Date.now()) return null;
      if (session.userId !== payload.sub || session.role !== role) return null;
      return { id: payload.sub, role };
    } catch {
      return null;
    }
  }

  private async issueSession(userId: string, phone: string, role: LoginRole) {
    const id = randomUUID();
    const refreshToken = randomBytes(32).toString('base64url');
    this.sessions.set(id, {
      id,
      userId,
      phone,
      role,
      refreshTokenHash: this.hash(refreshToken),
      expiresAt: Date.now() + 7 * 24 * 60 * 60 * 1000,
      revoked: false,
    });
    return {
      ...(await this.accessToken(userId, role, id)),
      refreshToken,
      user: { id: userId, phone, role },
    };
  }

  private async accessToken(userId: string, role: LoginRole, sessionId: string) {
    const accessToken = await new SignJWT({ role, sid: sessionId }).setProtectedHeader({ alg: 'HS256' })
      .setSubject(userId).setIssuer(this.issuer).setAudience('the-green-shop-api')
      .setIssuedAt().setExpirationTime('10m').sign(this.secret);
    return { accessToken, expiresIn: 600 };
  }

  private requireEnabled() {
    if (!this.enabled) throw new ForbiddenException('Development OTP authentication is disabled');
  }

  private allowedPhone(role: LoginRole) {
    return process.env[role === 'customer' ? 'DEV_CUSTOMER_TEST_PHONE_NUMBER'
      : role === 'admin' ? 'DEV_ADMIN_TEST_PHONE_NUMBER' : 'DEV_DELIVERY_PARTNER_TEST_PHONE_NUMBER'] ?? '';
  }

  private hash(value: string) { return createHash('sha256').update(value).digest('hex'); }
  private safeOtpMatch(value: string) {
    const expected = process.env.DEV_OTP_CODE ?? '';
    if (value.length !== expected.length) return false;
    return this.hash(value) === this.hash(expected);
  }
}
