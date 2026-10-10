import { Body, Controller, Get, Headers, Param, Post, RawBodyRequest, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { AuthGuard, AuthenticatedRequest } from '../auth/auth.guard';
import { CreateCheckoutDto, VerifyPaymentDto } from './dto';
import { PaymentsService } from './payments.service';

@Controller('payments')
export class PaymentsController {
  constructor(private service:PaymentsService){}
  @Post('webhooks/razorpay') webhook(@Req() req:RawBodyRequest<Request>,@Headers('x-razorpay-signature') signature='',@Headers('x-razorpay-event-id') eventId?:string){return this.service.webhook(req.rawBody??Buffer.alloc(0),signature,eventId);}
  @Post('checkout') @UseGuards(AuthGuard) checkout(@Req() req:AuthenticatedRequest,@Body() dto:CreateCheckoutDto){return this.service.checkout(req.user.id,dto);}
  @Post('attempts/:id/verify') @UseGuards(AuthGuard) verify(@Req() req:AuthenticatedRequest,@Param('id') id:string,@Body() dto:VerifyPaymentDto){return this.service.verify(req.user.id,id,dto);}
  @Get('attempts/:id') @UseGuards(AuthGuard) status(@Req() req:AuthenticatedRequest,@Param('id') id:string){return this.service.status(req.user.id,id);}
}
