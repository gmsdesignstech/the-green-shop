import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { createRemoteJWKSet, decodeJwt, jwtVerify } from 'jose';
import type { Request } from 'express';
import { DevAuthService } from '../dev-auth/dev-auth.service';

export type AuthenticatedRequest = Request & { user: { id: string; role?: 'customer'|'admin'|'delivery_partner' } };

@Injectable()
export class AuthGuard implements CanActivate {
  private readonly issuer: string;
  private readonly jwks: ReturnType<typeof createRemoteJWKSet>;
  private readonly supabaseUrl: string;
  private readonly publishableKey?: string;

  constructor(private readonly devAuth: DevAuthService) {
    const baseUrl = process.env.SUPABASE_URL?.replace(/\/$/, '');
    if (!baseUrl) throw new Error('SUPABASE_URL is required');
    this.supabaseUrl = baseUrl;
    this.publishableKey = process.env.SUPABASE_PUBLISHABLE_KEY;
    this.issuer = `${baseUrl}/auth/v1`;
    this.jwks = createRemoteJWKSet(new URL(`${this.issuer}/.well-known/jwks.json`));
  }

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<AuthenticatedRequest>();
    const [scheme, token] = request.headers.authorization?.split(' ') ?? [];
    if (scheme !== 'Bearer' || !token) throw new UnauthorizedException('Authentication required');
    let userId: string | undefined;
    let developmentRole: 'customer'|'admin'|'delivery_partner'|undefined;
    try {
      const { payload } = await jwtVerify(token, this.jwks, { issuer: this.issuer, audience: 'authenticated' });
      userId = payload.sub;
    } catch (localVerificationError) {
      const developmentUser=await this.devAuth.verifyAccessToken(token);
      userId=developmentUser?.id;
      developmentRole=developmentUser?.role;
      if(!userId) userId = await this.verifyWithAuthServer(token).catch(() => undefined);
    }
    if (!userId) {
      throw new UnauthorizedException('Invalid or expired session');
    }
    request.user = { id: userId, role: developmentRole };
    return true;
  }

  private async verifyWithAuthServer(token: string): Promise<string> {
    if (!this.publishableKey) throw new Error('SUPABASE_PUBLISHABLE_KEY is required for shared-secret token verification');
    const response = await fetch(`${this.supabaseUrl}/auth/v1/user`, {
      headers: { apikey: this.publishableKey, Authorization: `Bearer ${token}` },
      signal: AbortSignal.timeout(5_000),
    });
    if (!response.ok) throw new Error('Token rejected by Supabase Auth');
    const user = await response.json() as { id?: string };
    if (!user.id) throw new Error('Supabase Auth response did not contain a user id');

    // The Auth endpoint validates the signature/session; this local check prevents
    // accepting a token issued for a different audience if provider behavior changes.
    const payload = decodeJwt(token);
    if (payload.iss !== this.issuer || payload.aud !== 'authenticated' || payload.sub !== user.id) {
      throw new Error('Unexpected token claims');
    }
    return user.id;
  }
}
