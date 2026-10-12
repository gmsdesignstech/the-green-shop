import { ConflictException, ForbiddenException, NotFoundException, UnprocessableEntityException } from '@nestjs/common';
import { RiderService } from './rider.service';

describe('RiderService delivery safety',()=>{
 it('uses optimistic concurrency for availability',async()=>{const db={query:vi.fn().mockResolvedValue({rowCount:0,rows:[]})} as any;await expect(new RiderService(db).setAvailability('p',{isOnline:true,version:1})).rejects.toBeInstanceOf(ConflictException);});
 it('prevents access to another partner assignment',async()=>{const client={query:vi.fn().mockResolvedValue({rowCount:0,rows:[]})};const db={transaction:(fn:any)=>fn(client)} as any;await expect(new RiderService(db).action('p',9,'accepted',{version:1})).rejects.toBeInstanceOf(NotFoundException);expect(client.query.mock.calls[0][0]).toContain('partner_user_id=$2');});
 it('rejects skipping pickup lifecycle states',async()=>{const client={query:vi.fn().mockResolvedValueOnce({rowCount:1,rows:[{id:2,order_id:4,status:'accepted',version:1}]})};const db={transaction:(fn:any)=>fn(client)} as any;await expect(new RiderService(db).action('p',2,'out_for_delivery',{version:1})).rejects.toBeInstanceOf(UnprocessableEntityException);});
 it('requires online state before accepting',async()=>{const client={query:vi.fn().mockResolvedValueOnce({rowCount:1,rows:[{id:2,order_id:4,status:'assigned',version:1}]}).mockResolvedValueOnce({rowCount:1,rows:[{is_online:false}]})};const db={transaction:(fn:any)=>fn(client)} as any;await expect(new RiderService(db).action('p',2,'accepted',{version:1})).rejects.toBeInstanceOf(ForbiddenException);});
 it('rejects location sharing outside active transit states',async()=>{const db={query:vi.fn().mockResolvedValue({rowCount:0})} as any;await expect(new RiderService(db).locations('p',2,{points:[{latitude:1,longitude:2,recordedAt:new Date().toISOString()}]})).rejects.toBeInstanceOf(ForbiddenException);});
 it('rejects stale location samples for an active assignment',async()=>{const db={query:vi.fn().mockResolvedValueOnce({rowCount:1})} as any;await expect(new RiderService(db).locations('p',2,{points:[{latitude:1,longitude:2,recordedAt:new Date(Date.now()-11*60_000).toISOString()}]})).rejects.toBeInstanceOf(UnprocessableEntityException);});
 it('rejects the null island coordinate',async()=>{const db={query:vi.fn().mockResolvedValueOnce({rowCount:1})} as any;await expect(new RiderService(db).locations('p',2,{points:[{latitude:0,longitude:0,recordedAt:new Date().toISOString()}]})).rejects.toBeInstanceOf(UnprocessableEntityException);});
 it('keeps the rider dashboard available when an optional metrics query fails',async()=>{
  const db={query:vi.fn(async(text:string)=>{
   if(text.includes('delivery_partner_profiles'))return{rowCount:1,rows:[{display_name:'Test Partner',service_area:'Local',vehicle_type:'Bicycle',is_online:false,availability_version:1}]};
   if(text.includes('today_completed'))throw Object.assign(new Error('connection failed'),{code:'ECONNRESET'});
   if(text.includes('o.delivery_address'))return{rowCount:0,rows:[]};
   if(text.includes('partner_notifications'))return{rowCount:0,rows:[]};
   return{rowCount:0,rows:[]};
  })} as any;

  const result=await new RiderService(db).dashboard('partner-user');

  expect(result.profile.display_name).toBe('Test Partner');
  expect(result.activeAssignment).toBeNull();
  expect(result.summary.today_earnings).toBeNull();
  expect(result.unavailable).toContain('summary');
 });
 it('does not turn a missing rider profile into an empty dashboard',async()=>{
  const db={query:vi.fn().mockResolvedValue({rowCount:0,rows:[]})} as any;
  await expect(new RiderService(db).dashboard('unprovisioned')).rejects.toBeInstanceOf(NotFoundException);
 });
 it('provides an isolated empty dashboard and availability for the development partner',async()=>{
  const previousNodeEnv=process.env.NODE_ENV;
  const previousDevOtp=process.env.DEV_OTP_ENABLED;
  process.env.NODE_ENV='development';
  process.env.DEV_OTP_ENABLED='true';
  const db={query:vi.fn()} as any;
  try{
   const service=new RiderService(db);
   const dashboard=await service.dashboard('00000000-0000-4000-8000-000000000003');
   expect(dashboard.profile.display_name).toBe('Development Delivery Partner');
   expect(dashboard.activeAssignment).toBeNull();
   expect(dashboard.unavailable).toEqual([]);
   const availability=await service.setAvailability('00000000-0000-4000-8000-000000000003',{isOnline:true,version:1});
   expect(availability).toEqual(expect.objectContaining({is_online:true,version:2}));
   expect(db.query).not.toHaveBeenCalled();
  }finally{
   if(previousNodeEnv===undefined)delete process.env.NODE_ENV;else process.env.NODE_ENV=previousNodeEnv;
   if(previousDevOtp===undefined)delete process.env.DEV_OTP_ENABLED;else process.env.DEV_OTP_ENABLED=previousDevOtp;
  }
 });
});
