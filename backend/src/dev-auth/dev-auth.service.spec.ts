import { ForbiddenException, UnauthorizedException } from '@nestjs/common';
import { DevAuthService } from './dev-auth.service';

jest.mock('jose',()=>({
  SignJWT: class {
    setProtectedHeader(){return this;} setSubject(){return this;} setIssuer(){return this;}
    setAudience(){return this;} setIssuedAt(){return this;} setExpirationTime(){return this;}
    async sign(){return 'signed-development-token';}
  },
  jwtVerify:jest.fn(),
}));

describe('DevAuthService',()=>{
  const secret='development-test-secret-that-is-long-enough';
  beforeEach(()=>{process.env.NODE_ENV='development';process.env.DEV_OTP_ENABLED='true';process.env.DEV_OTP_CODE='123456';process.env.DEV_AUTH_JWT_SECRET=secret;process.env.DEV_CUSTOMER_TEST_PHONE_NUMBER='+919123456789';process.env.DEV_ADMIN_TEST_PHONE_NUMBER='+919876543210';process.env.DEV_DELIVERY_PARTNER_TEST_PHONE_NUMBER='+919876543210';});

  it('issues a short-lived session only for the authorized administrator number',async()=>{
    const result=await new DevAuthService().login('+919876543210','123456','admin');
    expect(result.user).toEqual({id:'00000000-0000-4000-8000-000000000002',phone:'+919876543210',role:'admin'});
    expect(result.expiresIn).toBe(600);
    expect(result.accessToken).toBeTruthy();expect(result.refreshToken).toBeTruthy();
  });

  it('creates an isolated customer development session',async()=>{
    const result=await new DevAuthService().login('+919123456789','123456','customer');
    expect(result.user).toEqual({id:'00000000-0000-4000-8000-000000000001',phone:'+919123456789',role:'customer'});
  });

  it('rejects an invalid OTP before account lookup',async()=>{
    await expect(new DevAuthService().login('+919876543210','000000','admin')).rejects.toBeInstanceOf(UnauthorizedException);
  });

  it('keeps role-specific development phone numbers separate',async()=>{
    const service=new DevAuthService();
    await expect(service.login('+919123456789','123456','admin')).rejects.toBeInstanceOf(ForbiddenException);
    await expect(service.login('+919876543210','123456','customer')).rejects.toBeInstanceOf(ForbiddenException);
  });

  it('rejects a registered phone that is not on the development allowlist',async()=>{
    await expect(new DevAuthService().login('+919111111111','123456','customer')).rejects.toBeInstanceOf(ForbiddenException);
  });

  it('refuses to start with development OTP enabled in production',()=>{
    process.env.NODE_ENV='production';
    expect(()=>new DevAuthService()).toThrow('DEV_OTP_ENABLED may only be enabled');
  });

  it('rejects the fixed OTP when development authentication is disabled',async()=>{
    process.env.DEV_OTP_ENABLED='false';
    await expect(new DevAuthService().login('+919123456789','123456','customer')).rejects.toBeInstanceOf(ForbiddenException);
  });
});
