import { ConflictException, ForbiddenException, HttpException, HttpStatus, Injectable, NotFoundException, UnprocessableEntityException } from '@nestjs/common';
import { createHash, timingSafeEqual } from 'crypto';
import { DatabaseService } from '../database.service';
import { AvailabilityDto, DeliveryActionDto, LocationBatchDto, RiderPageDto, SupportTicketDto } from './dto';

const next:Record<string,string[]>={assigned:['accepted'],accepted:['arrived_pickup'],arrived_pickup:['picked_up'],picked_up:['out_for_delivery'],out_for_delivery:['delivered','failed'],delivered:[],failed:[],cancelled:[]};

@Injectable()
export class RiderService {
  constructor(private readonly db:DatabaseService){}
  async dashboard(userId:string){
    const [profile,active,summary,recent,notices]=await Promise.all([
      this.db.query(`select p.display_name,p.service_area,p.vehicle_type,a.is_online,a.version availability_version
        from public.delivery_partner_profiles p join public.partner_availability a on a.partner_user_id=p.user_id where p.user_id=$1`,[userId]),
      this.db.query(`select a.id,a.order_id,a.status,a.version,a.pickup_name,a.pickup_address,a.pickup_instructions,a.delivery_instructions,
        o.delivery_address,o.updated_at from public.delivery_assignments a join public.orders o on o.id=a.order_id
        where a.partner_user_id=$1 and a.status not in ('delivered','failed','cancelled') order by a.assigned_at desc limit 1`,[userId]),
      this.db.query(`select
        (select count(*)::int from public.delivery_assignments where partner_user_id=$1 and status='delivered' and completed_at::date=current_date) today_completed,
        (select coalesce(sum(base_amount+incentive_amount+adjustment_amount),0) from public.partner_earnings where partner_user_id=$1 and status in ('earned','scheduled','paid') and earned_at::date=current_date) today_earnings,
        (select coalesce(sum(base_amount+incentive_amount+adjustment_amount),0) from public.partner_earnings where partner_user_id=$1 and status in ('earned','scheduled')) pending_earnings,
        (select coalesce(sum(amount),0) from public.partner_payouts where partner_user_id=$1 and status in ('pending','processing')) pending_payout`,[userId]),
      this.db.query(`select id,order_id,status,assigned_at,completed_at from public.delivery_assignments where partner_user_id=$1 order by assigned_at desc limit 5`,[userId]),
      this.db.query(`select id,type,title,message,read_at,created_at from public.partner_notifications where partner_user_id=$1 order by created_at desc limit 5`,[userId]),
    ]);
    if(!profile.rowCount) throw new NotFoundException('Partner profile is not provisioned');
    return{profile:profile.rows[0],activeAssignment:active.rows[0]??null,summary:summary.rows[0],recentDeliveries:recent.rows,notifications:notices.rows};
  }
  async setAvailability(userId:string,dto:AvailabilityDto){
    const result=await this.db.query(`update public.partner_availability set is_online=$1,version=version+1,updated_at=now()
      where partner_user_id=$2 and version=$3 returning is_online,version,updated_at`,[dto.isOnline,userId,dto.version]);
    if(!result.rowCount) throw new ConflictException('Availability changed on another device; refresh and retry');
    return result.rows[0];
  }
  async deliveries(userId:string,q:RiderPageDto){const cursor=this.cursor(q.cursor);const result=await this.db.query(`select a.id,a.order_id,a.status,a.version,a.pickup_name,a.assigned_at,a.accepted_at,a.picked_up_at,a.completed_at,
    coalesce(e.base_amount+e.incentive_amount+e.adjustment_amount,0) earnings from public.delivery_assignments a left join public.partner_earnings e on e.assignment_id=a.id
    where a.partner_user_id=$1 and a.id<$2 and ($3::text is null or a.status=$3) order by a.id desc limit $4`,[userId,cursor,q.status??null,q.limit+1]);return this.page(result.rows,q.limit);}
  async detail(userId:string,id:number){const result=await this.db.query(`select a.*,o.delivery_address,o.status order_status,
    jsonb_agg(jsonb_build_object('name',oi.product_name,'quantity',oi.quantity) order by oi.id) filter(where oi.id is not null) items
    from public.delivery_assignments a join public.orders o on o.id=a.order_id left join public.order_items oi on oi.order_id=o.id
    where a.id=$1 and a.partner_user_id=$2 group by a.id,o.id`,[id,userId]);if(!result.rowCount)throw new NotFoundException('Assigned delivery not found');return result.rows[0];}
  action(userId:string,id:number,target:string,dto:DeliveryActionDto){return this.db.transaction(async client=>{
    const found=await client.query('select id,order_id,status,version from public.delivery_assignments where id=$1 and partner_user_id=$2 for update',[id,userId]);
    if(!found.rowCount) throw new NotFoundException('Assigned delivery not found'); const current=found.rows[0];
    if(current.version!==dto.version) throw new ConflictException('Delivery changed; refresh and retry');
    if(!next[current.status]?.includes(target)) throw new UnprocessableEntityException(`Invalid transition from ${current.status} to ${target}`);
    if(target==='accepted'){const online=await client.query('select is_online from public.partner_availability where partner_user_id=$1',[userId]);if(!online.rows[0]?.is_online)throw new ForbiddenException('Go online before accepting a delivery');}
    if(target==='failed'&&!dto.reason?.trim())throw new UnprocessableEntityException('A failed-delivery reason is required');
    if(target==='delivered') await this.verifyOtp(client,id,dto.otp,dto.idempotencyKey);
    const updated=await client.query(`update public.delivery_assignments set status=$1,version=version+1,
      accepted_at=case when $1='accepted' then now() else accepted_at end,picked_up_at=case when $1='picked_up' then now() else picked_up_at end,
      completed_at=case when $1 in ('delivered','failed') then now() else completed_at end,failure_reason=case when $1='failed' then $2 else failure_reason end,updated_at=now()
      where id=$3 returning *`,[target,dto.reason?.trim()??null,id]);
    const orderStatus=target==='picked_up'?'rider_assigned':target==='out_for_delivery'?'out_for_delivery':target==='delivered'?'delivered':null;
    if(orderStatus)await client.query('update public.orders set status=$1,version=version+1,updated_at=now() where id=$2',[orderStatus,current.order_id]);
    await client.query('insert into public.delivery_assignment_history(assignment_id,from_status,to_status,actor_user_id,reason) values($1,$2,$3,$4,$5)',[id,current.status,target,userId,dto.reason??null]);
    await client.query('insert into public.audit_logs(actor_user_id,action,target_type,target_id,metadata) values($1,$2,$3,$4,$5::jsonb)',[userId,`delivery.${target}`,'delivery_assignment',String(id),JSON.stringify({from:current.status})]);
    return updated.rows[0];
  });}
  async locations(userId:string,id:number,dto:LocationBatchDto){if(!dto.points.length)return{accepted:0};const active=await this.db.query("select 1 from public.delivery_assignments where id=$1 and partner_user_id=$2 and status in ('picked_up','out_for_delivery')",[id,userId]);if(!active.rowCount)throw new ForbiddenException('Location sharing is not permitted for this delivery state');
    const now=Date.now();for(const p of dto.points){const t=new Date(p.recordedAt);if(Number.isNaN(t.valueOf())||t.valueOf()<now-10*60_000||t.valueOf()>now+60_000)throw new UnprocessableEntityException('Location timestamp is invalid or stale');if(p.latitude===0&&p.longitude===0)throw new UnprocessableEntityException('Location coordinates are invalid');}
    let accepted=0;for(const p of dto.points){const result=await this.db.query(`insert into public.partner_location_updates(assignment_id,partner_user_id,latitude,longitude,accuracy_meters,heading_degrees,speed_mps,recorded_at)
      values($1,$2,$3,$4,$5,$6,$7,$8) on conflict do nothing`,[id,userId,p.latitude,p.longitude,p.accuracyMeters??null,p.headingDegrees??null,p.speedMps??null,new Date(p.recordedAt)]);accepted+=result.rowCount??0;}return{accepted};}
  async earnings(userId:string,q:RiderPageDto){const cursor=this.cursor(q.cursor);const summary=await this.db.query(`select coalesce(sum(base_amount+incentive_amount+adjustment_amount) filter(where earned_at::date=current_date),0) today,
    coalesce(sum(base_amount+incentive_amount+adjustment_amount) filter(where earned_at>=date_trunc('week',now())),0) week,
    coalesce(sum(base_amount+incentive_amount+adjustment_amount) filter(where earned_at>=date_trunc('month',now())),0) month,
    coalesce(sum(base_amount+incentive_amount+adjustment_amount) filter(where status in ('earned','scheduled')),0) unpaid from public.partner_earnings where partner_user_id=$1`,[userId]);const rows=await this.db.query('select id,assignment_id,base_amount,incentive_amount,adjustment_amount,status,earned_at from public.partner_earnings where partner_user_id=$1 and id<$2 order by id desc limit $3',[userId,cursor,q.limit+1]);return{summary:summary.rows[0],...this.page(rows.rows,q.limit)};}
  async payouts(userId:string,q:RiderPageDto){const cursor=this.cursor(q.cursor);const rows=await this.db.query('select id,amount,status,destination_mask,initiated_at,completed_at from public.partner_payouts where partner_user_id=$1 and id<$2 order by id desc limit $3',[userId,cursor,q.limit+1]);return this.page(rows.rows,q.limit);}
  async notifications(userId:string,q:RiderPageDto){const cursor=this.cursor(q.cursor);const rows=await this.db.query('select id,type,title,message,assignment_id,read_at,created_at from public.partner_notifications where partner_user_id=$1 and id<$2 order by id desc limit $3',[userId,cursor,q.limit+1]);return this.page(rows.rows,q.limit);}
  async support(userId:string,dto:SupportTicketDto){if(dto.assignmentId){const owned=await this.db.query('select 1 from public.delivery_assignments where id=$1 and partner_user_id=$2',[dto.assignmentId,userId]);if(!owned.rowCount)throw new ForbiddenException('Delivery is not assigned to this partner');}const result=await this.db.query('insert into public.partner_support_tickets(partner_user_id,assignment_id,category,description) values($1,$2,$3,$4) returning id,status,created_at',[userId,dto.assignmentId??null,dto.category,dto.description.trim()]);return result.rows[0];}
  private async verifyOtp(client:any,id:number,otp?:string,key?:string){if(!key)throw new UnprocessableEntityException('Idempotency key is required');const v=await client.query('select otp_hash,failed_attempts,expires_at,verified_at from public.delivery_verifications where assignment_id=$1 for update',[id]);if(!v.rowCount)throw new UnprocessableEntityException('Delivery verification is not configured for this assignment');if(v.rows[0].verified_at)return;if(v.rows[0].failed_attempts>=5)throw new HttpException('Too many verification attempts',HttpStatus.TOO_MANY_REQUESTS);if(new Date(v.rows[0].expires_at)<new Date())throw new UnprocessableEntityException('Delivery verification code expired');const actual=createHash('sha256').update(otp??'').digest();const expected=Buffer.from(v.rows[0].otp_hash,'hex');if(actual.length!==expected.length||!timingSafeEqual(actual,expected)){await client.query('update public.delivery_verifications set failed_attempts=failed_attempts+1 where assignment_id=$1',[id]);throw new UnprocessableEntityException('Invalid delivery verification code');}await client.query('update public.delivery_verifications set verified_at=now() where assignment_id=$1',[id]);}
  private cursor(v?:string){const n=v?Number(v):Number.MAX_SAFE_INTEGER;if(!Number.isSafeInteger(n)||n<1)throw new UnprocessableEntityException('Invalid cursor');return n;}
  private page(rows:any[],limit:number){const more=rows.length>limit;const items=more?rows.slice(0,limit):rows;return{items,nextCursor:more?String(items[items.length-1].id):null};}
}
