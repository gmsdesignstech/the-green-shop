import { CanActivate, ExecutionContext, ForbiddenException, Injectable } from '@nestjs/common';
import { AuthenticatedRequest } from '../auth/auth.guard';
import { DatabaseService } from '../database.service';

@Injectable()
export class AdminGuard implements CanActivate {
  constructor(private readonly db: DatabaseService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<AuthenticatedRequest>();
    if (request.user.role === 'admin') return true;
    if (request.user.role) throw new ForbiddenException('Active administrator access is required');
    const result = await this.db.query(
      "select 1 from public.trusted_role_assignments where user_id=$1 and role='admin' and status='active'",
      [request.user.id],
    );
    if (!result.rowCount) throw new ForbiddenException('Active administrator access is required');
    return true;
  }
}
