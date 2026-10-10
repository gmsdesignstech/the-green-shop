import { Body, Controller, Get, Param, ParseIntPipe, Patch, Post, Query, Req, UseGuards } from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { AuthGuard, AuthenticatedRequest } from '../auth/auth.guard';
import { AvailabilityDto, DeliveryActionDto, LocationBatchDto, RiderPageDto, SupportTicketDto } from './dto';
import { RiderGuard } from './rider.guard';
import { RiderService } from './rider.service';

@Controller('rider') @UseGuards(AuthGuard,RiderGuard)
export class RiderController{
 constructor(private readonly service:RiderService){}
 @Get('me') dashboard(@Req() r:AuthenticatedRequest){return this.service.dashboard(r.user.id);}
 @Patch('me/availability') availability(@Req() r:AuthenticatedRequest,@Body() d:AvailabilityDto){return this.service.setAvailability(r.user.id,d);}
 @Get('deliveries') deliveries(@Req() r:AuthenticatedRequest,@Query() q:RiderPageDto){return this.service.deliveries(r.user.id,q);}
 @Get('deliveries/:id') detail(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number){return this.service.detail(r.user.id,id);}
 @Post('deliveries/:id/locations') @Throttle({default:{limit:12,ttl:60_000}}) locations(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number,@Body() d:LocationBatchDto){return this.service.locations(r.user.id,id,d);}
 @Post('deliveries/:id/:action') action(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number,@Param('action') action:string,@Body() d:DeliveryActionDto){return this.service.action(r.user.id,id,action.replace(/-/g,'_'),d);}
 @Get('earnings') earnings(@Req() r:AuthenticatedRequest,@Query() q:RiderPageDto){return this.service.earnings(r.user.id,q);}
 @Get('payouts') payouts(@Req() r:AuthenticatedRequest,@Query() q:RiderPageDto){return this.service.payouts(r.user.id,q);}
 @Get('notifications') notifications(@Req() r:AuthenticatedRequest,@Query() q:RiderPageDto){return this.service.notifications(r.user.id,q);}
 @Post('support') support(@Req() r:AuthenticatedRequest,@Body() d:SupportTicketDto){return this.service.support(r.user.id,d);}
}
