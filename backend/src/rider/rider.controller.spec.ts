import { Logger, NotFoundException, ServiceUnavailableException } from '@nestjs/common';
import { RiderController } from './rider.controller';

describe('RiderController database failures',()=>{
  afterEach(()=>jest.restoreAllMocks());

  it('logs partner and stack context and returns 503 for a database connection failure',async()=>{
    const logger=jest.spyOn(Logger.prototype,'error').mockImplementation();
    const service={dashboard:jest.fn().mockRejectedValue(Object.assign(new Error('certificate verify failed'),{code:'SELF_SIGNED_CERT_IN_CHAIN'}))};
    const controller=new RiderController(service as any);

    await expect(controller.dashboard({user:{id:'partner-user'}} as any))
      .rejects.toBeInstanceOf(ServiceUnavailableException);
    expect(logger).toHaveBeenCalledWith(
      'Delivery request GET /rider/me failed for partner partner-user',
      expect.stringContaining('certificate verify failed')
    );
  });

  it('preserves a partner-specific 404 rather than converting it to a server error',async()=>{
    const service={dashboard:jest.fn().mockRejectedValue(new NotFoundException('Partner profile is not provisioned'))};
    const controller=new RiderController(service as any);

    await expect(controller.dashboard({user:{id:'partner-user'}} as any))
      .rejects.toThrow('Partner profile is not provisioned');
  });
});
