import { ForbiddenException, UnauthorizedException } from '@nestjs/common';
import { DEVELOPMENT_USER_IDS, DevAuthService } from './dev-auth.service';

describe('DevAuthService',()=>{
  const secret='development-test-secret-that-is-long-enough';
  beforeEach(()=>{process.env.NODE_ENV='development';process.env.DEV_OTP_ENABLED='true';process.env.DEV_OTP_CODE='123456';delete process.env.DEV_OTP_VALID_UNTIL;process.env.DEV_AUTH_JWT_SECRET=secret;process.env.DEV_CUSTOMER_TEST_PHONE_NUMBER='+919000000001';process.env.DEV_ADMIN_TEST_PHONE_NUMBER='+919000000002';process.env.DEV_DELIVERY_PARTNER_TEST_PHONE_NUMBER='+919000000003';});

  it('issues a valid short-lived admin session in the client response format',async()=>{
    const service=new DevAuthService();
    const result=await service.login('+919000000002','123456','admin');
    expect(result.user).toEqual({id:'00000000-0000-4000-8000-000000000002',phone:'+919000000002',role:'admin'});
    expect(result.expiresIn).toBe(600);
    expect(result.accessToken).toBeTruthy();
    expect(result.refreshToken).toBeTruthy();
    await expect(service.verifyAccessToken(result.accessToken)).resolves.toEqual({
      id: result.user.id,
      role: 'admin',
    });
  });

  it('creates an isolated customer development session',async()=>{
    const service=new DevAuthService();
    const result=await service.login('+919000000001','123456','customer');
    expect(result.user).toEqual({id:'00000000-0000-4000-8000-000000000001',phone:'+919000000001',role:'customer'});
    await expect(service.verifyAccessToken(result.accessToken)).resolves.toEqual({
      id: result.user.id,
      role: 'customer',
    });
  });

  it('creates an isolated delivery-partner development session',async()=>{
    const service=new DevAuthService();
    const result=await service.login('+919000000003','123456','delivery_partner');
    expect(result.user).toEqual({id:'00000000-0000-4000-8000-000000000003',phone:'+919000000003',role:'delivery_partner'});
    await expect(service.verifyAccessToken(result.accessToken)).resolves.toEqual({
      id: result.user.id,
      role: 'delivery_partner',
    });
  });

  it('rejects an invalid OTP before account lookup',async()=>{
    await expect(new DevAuthService().login('+919000000002','000000','admin')).rejects.toBeInstanceOf(UnauthorizedException);
  });

  it('keeps role-specific development phone numbers separate',async()=>{
    const service=new DevAuthService();
    await expect(service.login('+919000000001','123456','admin')).rejects.toBeInstanceOf(ForbiddenException);
    await expect(service.login('+919000000002','123456','delivery_partner')).rejects.toBeInstanceOf(ForbiddenException);
    await expect(service.login('+919000000003','123456','customer')).rejects.toBeInstanceOf(ForbiddenException);
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

  it('locks one test identity after five incorrect OTP attempts',async()=>{
    const service=new DevAuthService();
    for(let attempt=0;attempt<5;attempt+=1){
      await expect(service.login('+919000000001','000000','customer')).rejects.toBeInstanceOf(UnauthorizedException);
    }
    await expect(service.login('+919000000001','123456','customer'))
      .rejects.toThrow('Too many development OTP attempts');
    // Lockout is scoped to the identity and does not block another allow-listed role.
    await expect(service.login('+919000000002','123456','admin')).resolves.toBeDefined();
  });

  it('rejects an expired development OTP configuration',async()=>{
    process.env.DEV_OTP_VALID_UNTIL='2020-01-01T00:00:00Z';
    await expect(new DevAuthService().login('+919000000001','123456','customer'))
      .rejects.toThrow('Development OTP has expired');
  });

  it('rejects an invalid development OTP expiry during startup',()=>{
    process.env.DEV_OTP_VALID_UNTIL='not-a-date';
    expect(()=>new DevAuthService()).toThrow('DEV_OTP_VALID_UNTIL must be a valid ISO-8601 timestamp');
  });

  it('revokes the previous session when login is repeated',async()=>{
    const service=new DevAuthService();
    const first=await service.login('+919000000001','123456','customer');
    const second=await service.login('+919000000001','123456','customer');
    await expect(service.verifyAccessToken(first.accessToken)).resolves.toBeNull();
    await expect(service.verifyAccessToken(second.accessToken)).resolves.toEqual({
      id: DEVELOPMENT_USER_IDS.customer,
      role: 'customer',
    });
  });
});
