import { ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { DatabaseService } from '../database.service';
import { AdminAccessRequestDto, PartnerApplicationDto, ReviewApplicationDto } from './dto';

@Injectable()
export class ApplicationsService {
  constructor(private readonly db: DatabaseService) {}

  async getMyAccess(userId: string) {
    const result = await this.db.query(
      `select
        exists(select 1 from public.trusted_role_assignments
          where user_id=$1 and role='admin' and status='active') as admin,
        exists(select 1 from public.trusted_role_assignments r
          join public.delivery_partner_applications a on a.user_id=r.user_id and a.status='approved'
          where r.user_id=$1 and r.role='delivery_partner' and r.status='active') as delivery_partner,
        (select status from public.admin_access_requests where user_id=$1 order by created_at desc limit 1) as admin_status,
        (select status from public.delivery_partner_applications where user_id=$1 order by created_at desc limit 1) as partner_status`,
      [userId],
    );
    return result.rows[0];
  }

  async createAdminRequest(userId: string, dto: AdminAccessRequestDto) {
    try {
      const result = await this.db.query(
        `insert into public.admin_access_requests
          (user_id, full_name, email, phone, business_name, reason)
         values ($1,$2,lower($3),$4,$5,$6)
         returning id, status, created_at, updated_at`,
        [userId, dto.fullName.trim(), dto.email.trim(), dto.phone, dto.businessName.trim(), dto.reason.trim()],
      );
      return result.rows[0];
    } catch (error: any) {
      if (error?.code === '23505') throw new ConflictException('An active admin access request already exists');
      throw error;
    }
  }

  async getMyAdminRequest(userId: string) {
    const result = await this.db.query(
      'select id, full_name, business_name, status, decision_reason, created_at, updated_at from public.admin_access_requests where user_id=$1 order by created_at desc limit 1',
      [userId],
    );
    if (!result.rowCount) throw new NotFoundException('No admin access request found');
    return result.rows[0];
  }

  async createPartnerApplication(userId: string, dto: PartnerApplicationDto) {
    if (!dto.consent) throw new ForbiddenException('Consent is required');
    try {
      const result = await this.db.query(
        `insert into public.delivery_partner_applications
          (user_id, full_name, phone, email, service_area, preferred_service_area, vehicle_type, driving_licence_number, vehicle_registration_number, consented_at)
         values ($1,$2,$3,lower($4),$5,$5,$6,$7,$8,now())
         returning id, status, created_at, updated_at`,
        [userId, dto.fullName.trim(), dto.phone, dto.email?.trim() ?? null, dto.serviceArea.trim(), dto.vehicleType,
          dto.drivingLicenceNumber?.trim() ?? null, dto.vehicleRegistrationNumber?.trim() ?? null],
      );
      return result.rows[0];
    } catch (error: any) {
      if (error?.code === '23505') throw new ConflictException('An active delivery partner application already exists');
      throw error;
    }
  }

  async getMyPartnerApplication(userId: string) {
    const result = await this.db.query(
      'select id, full_name, service_area, vehicle_type, status, decision_reason, created_at, updated_at from public.delivery_partner_applications where user_id=$1 order by created_at desc limit 1',
      [userId],
    );
    if (!result.rowCount) throw new NotFoundException('No delivery partner application found');
    return result.rows[0];
  }

  async listForAdmin(adminId: string, kind: string) {
    await this.requireAdmin(adminId);
    const table = this.tableFor(kind);
    const result = await this.db.query(`select * from public.${table} order by created_at asc limit 100`);
    return { items: result.rows, nextCursor: null };
  }

  async review(adminId: string, kind: string, id: number, dto: ReviewApplicationDto) {
    await this.requireAdmin(adminId);
    if (dto.status === 'rejected' && !dto.reason?.trim()) throw new ForbiddenException('A rejection reason is required');
    const table = this.tableFor(kind);
    const role = kind === 'admin' ? 'admin' : 'delivery_partner';
    return this.db.transaction(async (client) => {
      const found = await client.query(`select user_id, status from public.${table} where id=$1 for update`, [id]);
      if (!found.rowCount) throw new NotFoundException('Application not found');
      if (found.rows[0].user_id === adminId) throw new ForbiddenException('You cannot review your own application');
      const updated = await client.query(
        `update public.${table} set status=$1, decision_reason=$2, reviewed_by=$3, reviewed_at=now(), updated_at=now() where id=$4 returning *`,
        [dto.status, dto.reason?.trim() ?? null, adminId, id],
      );
      if (dto.status === 'approved') {
        await client.query(
          `insert into public.trusted_role_assignments(user_id, role, status, granted_by)
           values ($1,$2,'active',$3)
           on conflict (user_id, role) do update set status='active', granted_by=excluded.granted_by, updated_at=now()`,
          [found.rows[0].user_id, role, adminId],
        );
        if (kind === 'delivery_partner') {
          await client.query(
            `insert into public.delivery_partner_profiles(user_id,application_id,display_name,service_area,vehicle_type)
             select user_id,id,full_name,service_area,vehicle_type from public.delivery_partner_applications where id=$1
             on conflict(user_id) do update set application_id=excluded.application_id,display_name=excluded.display_name,
               service_area=excluded.service_area,vehicle_type=excluded.vehicle_type,updated_at=now()`, [id],
          );
          await client.query(
            `insert into public.partner_availability(partner_user_id,is_online) values($1,false)
             on conflict(partner_user_id) do nothing`, [found.rows[0].user_id],
          );
        }
      } else if (dto.status === 'suspended') {
        await client.query("update public.trusted_role_assignments set status='suspended', updated_at=now() where user_id=$1 and role=$2", [found.rows[0].user_id, role]);
      }
      await client.query(
        'insert into public.audit_logs(actor_user_id, action, target_type, target_id, metadata) values ($1,$2,$3,$4,$5::jsonb)',
        [adminId, `application.${dto.status}`, kind, String(id), JSON.stringify({ reason: dto.reason ?? null })],
      );
      return updated.rows[0];
    });
  }

  private tableFor(kind: string) {
    if (kind === 'admin') return 'admin_access_requests';
    if (kind === 'delivery_partner') return 'delivery_partner_applications';
    throw new NotFoundException('Unknown application type');
  }

  private async requireAdmin(userId: string) {
    const result = await this.db.query(
      "select 1 from public.trusted_role_assignments where user_id=$1 and role='admin' and status='active'",
      [userId],
    );
    if (!result.rowCount) throw new ForbiddenException('Active administrator access is required');
  }
}
