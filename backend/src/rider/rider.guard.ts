import { CanActivate, ExecutionContext, ForbiddenException, Injectable, Logger, ServiceUnavailableException } from '@nestjs/common';
import { AuthenticatedRequest } from '../auth/auth.guard';
import { DatabaseService } from '../database.service';

@Injectable()
export class RiderGuard implements CanActivate {
  private readonly logger = new Logger(RiderGuard.name);

  constructor(private readonly db: DatabaseService) {}
  async canActivate(context: ExecutionContext) {
    const request=context.switchToHttp().getRequest<AuthenticatedRequest>();
    if(request.user.role==='delivery_partner') return true;
    if(request.user.role) throw new ForbiddenException('An approved, active delivery partner account is required');
    let result;
    try {
      result=await this.db.query(`select 1 from public.trusted_role_assignments r
        join public.delivery_partner_applications a on a.user_id=r.user_id and a.status='approved'
        where r.user_id=$1 and r.role='delivery_partner' and r.status='active'`,[request.user.id]);
    } catch(error) {
      this.logger.error(`Failed to authorize delivery partner ${request.user.id}`,error instanceof Error?error.stack:String(error));
      throw new ServiceUnavailableException('Delivery partner access could not be checked because the database is unavailable.');
    }
    if(!result.rowCount) throw new ForbiddenException('An approved, active delivery partner account is required');
    return true;
  }
}
