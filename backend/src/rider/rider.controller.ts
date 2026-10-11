import { Body, Controller, Get, HttpException, Logger, Param, ParseIntPipe, Patch, Post, Query, Req, ServiceUnavailableException, UseGuards } from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { AuthGuard, AuthenticatedRequest } from '../auth/auth.guard';
import { AvailabilityDto, DeliveryActionDto, LocationBatchDto, RiderPageDto, SupportTicketDto } from './dto';
import { RiderGuard } from './rider.guard';
import { RiderService } from './rider.service';

@Controller('rider') @UseGuards(AuthGuard,RiderGuard)
export class RiderController{
 private readonly logger = new Logger(RiderController.name);

 constructor(private readonly service:RiderService){}
 @Get('me') dashboard(@Req() r:AuthenticatedRequest){return this.run('GET /rider/me',r.user.id,()=>this.service.dashboard(r.user.id));}
 @Patch('me/availability') availability(@Req() r:AuthenticatedRequest,@Body() d:AvailabilityDto){return this.run('PATCH /rider/me/availability',r.user.id,()=>this.service.setAvailability(r.user.id,d));}
 @Get('deliveries') deliveries(@Req() r:AuthenticatedRequest,@Query() q:RiderPageDto){return this.run('GET /rider/deliveries',r.user.id,()=>this.service.deliveries(r.user.id,q));}
 @Get('deliveries/:id') detail(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number){return this.run(`GET /rider/deliveries/${id}`,r.user.id,()=>this.service.detail(r.user.id,id));}
 @Post('deliveries/:id/locations') @Throttle({default:{limit:12,ttl:60_000}}) locations(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number,@Body() d:LocationBatchDto){return this.run(`POST /rider/deliveries/${id}/locations`,r.user.id,()=>this.service.locations(r.user.id,id,d));}
 @Post('deliveries/:id/:action') action(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number,@Param('action') action:string,@Body() d:DeliveryActionDto){return this.run(`POST /rider/deliveries/${id}/${action}`,r.user.id,()=>this.service.action(r.user.id,id,action.replace(/-/g,'_'),d));}
 @Get('earnings') earnings(@Req() r:AuthenticatedRequest,@Query() q:RiderPageDto){return this.run('GET /rider/earnings',r.user.id,()=>this.service.earnings(r.user.id,q));}
 @Get('payouts') payouts(@Req() r:AuthenticatedRequest,@Query() q:RiderPageDto){return this.run('GET /rider/payouts',r.user.id,()=>this.service.payouts(r.user.id,q));}
 @Get('notifications') notifications(@Req() r:AuthenticatedRequest,@Query() q:RiderPageDto){return this.run('GET /rider/notifications',r.user.id,()=>this.service.notifications(r.user.id,q));}
 @Post('support') support(@Req() r:AuthenticatedRequest,@Body() d:SupportTicketDto){return this.run('POST /rider/support',r.user.id,()=>this.service.support(r.user.id,d));}

 private async run<T>(route:string,userId:string,operation:()=>Promise<T>):Promise<T>{
  try{return await operation();}
  catch(error){
   if(!(error instanceof HttpException)||error.getStatus()>=500){
    this.logger.error(`Delivery request ${route} failed for partner ${userId}`,error instanceof Error?error.stack:String(error));
   }
   if(this.isDatabaseUnavailable(error)){
    throw new ServiceUnavailableException('Delivery service database is temporarily unavailable. Check database connectivity, then retry.');
   }
   throw error;
  }
 }

 private isDatabaseUnavailable(error:unknown):boolean{
  const transientCodes=new Set(['ECONNREFUSED','ECONNRESET','ETIMEDOUT','SELF_SIGNED_CERT_IN_CHAIN','CERT_HAS_EXPIRED','UNABLE_TO_VERIFY_LEAF_SIGNATURE','08001','08006','57P01']);
  let current=error;
  while(current&&typeof current==='object'){
   const candidate=current as {code?:unknown;message?:unknown;cause?:unknown};
   if(typeof candidate.code==='string'&&transientCodes.has(candidate.code))return true;
   if(typeof candidate.message==='string'&&/certificate|database connection|connection terminated/i.test(candidate.message))return true;
   current=candidate.cause;
  }
  return false;
 }
}
