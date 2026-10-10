import { Controller, Get, Param, ParseIntPipe, Req, UseGuards } from '@nestjs/common';
import { AdminGuard } from '../admin/admin.guard';
import { AuthGuard, AuthenticatedRequest } from '../auth/auth.guard';
import { TrackingService } from './tracking.service';

@Controller('tracking')
@UseGuards(AuthGuard)
export class TrackingController {
  constructor(private readonly tracking: TrackingService) {}

  @Get('orders/:id')
  snapshot(@Req() request: AuthenticatedRequest, @Param('id', ParseIntPipe) id: number) {
    return this.tracking.customerSnapshot(request.user.id, id);
  }

  @Get('admin/active-deliveries')
  @UseGuards(AdminGuard)
  activeDeliveries() {
    return this.tracking.adminActiveDeliveries();
  }
}
