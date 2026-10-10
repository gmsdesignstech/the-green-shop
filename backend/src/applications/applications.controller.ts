import { Body, Controller, Get, Param, ParseIntPipe, Patch, Post, Req, UseGuards } from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { AuthGuard, AuthenticatedRequest } from '../auth/auth.guard';
import { ApplicationsService } from './applications.service';
import { AdminAccessRequestDto, PartnerApplicationDto, ReviewApplicationDto } from './dto';

@Controller()
@UseGuards(AuthGuard)
export class ApplicationsController {
  constructor(private readonly applications: ApplicationsService) {}

  @Get('access/me')
  access(@Req() req: AuthenticatedRequest) {
    if (req.user.role) {
      return {
        admin: req.user.role === 'admin',
        delivery_partner: req.user.role === 'delivery_partner',
        admin_status: req.user.role === 'admin' ? 'approved' : null,
        partner_status: req.user.role === 'delivery_partner' ? 'approved' : null,
      };
    }
    return this.applications.getMyAccess(req.user.id);
  }

  @Post('admin/access-requests')
  @Throttle({ default: { limit: 3, ttl: 3_600_000 } })
  createAdmin(@Req() req: AuthenticatedRequest, @Body() dto: AdminAccessRequestDto) {
    return this.applications.createAdminRequest(req.user.id, dto);
  }

  @Get('admin/access-requests/me')
  getMyAdmin(@Req() req: AuthenticatedRequest) { return this.applications.getMyAdminRequest(req.user.id); }

  @Post('delivery-partners/applications')
  @Throttle({ default: { limit: 3, ttl: 3_600_000 } })
  createPartner(@Req() req: AuthenticatedRequest, @Body() dto: PartnerApplicationDto) {
    return this.applications.createPartnerApplication(req.user.id, dto);
  }

  @Get('delivery-partners/applications/me')
  getMyPartner(@Req() req: AuthenticatedRequest) { return this.applications.getMyPartnerApplication(req.user.id); }

  @Get('admin/applications/:kind')
  list(@Req() req: AuthenticatedRequest, @Param('kind') kind: string) {
    return this.applications.listForAdmin(req.user.id, kind);
  }

  @Patch('admin/applications/:kind/:id')
  review(
    @Req() req: AuthenticatedRequest,
    @Param('kind') kind: string,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: ReviewApplicationDto,
  ) { return this.applications.review(req.user.id, kind, id, dto); }
}
