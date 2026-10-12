import { Body, Controller, Delete, Get, Param, ParseIntPipe, Patch, Post, Query, Req, UseGuards } from '@nestjs/common';
import { AuthGuard, AuthenticatedRequest } from '../auth/auth.guard';
import { CustomerService } from './customer.service';
import { CustomerAddressDto, CustomerOrdersQueryDto } from './dto';

@Controller('customer')
@UseGuards(AuthGuard)
export class CustomerController {
  constructor(private readonly service: CustomerService) {}

  @Get('orders')
  orders(@Req() request: AuthenticatedRequest, @Query() query: CustomerOrdersQueryDto) {
    return this.service.orders(request.user.id, query);
  }

  @Get('orders/:id')
  order(@Req() request: AuthenticatedRequest, @Param('id', ParseIntPipe) id: number) {
    return this.service.order(request.user.id, id);
  }

  @Get('addresses') addresses(@Req() request: AuthenticatedRequest) {
    return this.service.addresses(request.user.id);
  }

  @Post('addresses') createAddress(@Req() request: AuthenticatedRequest, @Body() dto: CustomerAddressDto) {
    return this.service.createAddress(request.user.id, dto);
  }

  @Patch('addresses/:id') updateAddress(@Req() request: AuthenticatedRequest, @Param('id', ParseIntPipe) id: number, @Body() dto: CustomerAddressDto) {
    return this.service.updateAddress(request.user.id, id, dto);
  }

  @Delete('addresses/:id') deleteAddress(@Req() request: AuthenticatedRequest, @Param('id', ParseIntPipe) id: number) {
    return this.service.deleteAddress(request.user.id, id);
  }
}
