import { Injectable, NotFoundException } from '@nestjs/common';
import { DatabaseService } from '../database.service';

@Injectable()
export class TrackingService {
  constructor(private readonly db: DatabaseService) {}

  async customerSnapshot(userId: string, orderId: number) {
    const result = await this.db.query(
      `select o.id order_id,o.status order_status,o.delivery_address,a.id assignment_id,a.status delivery_status,
        l.latitude,l.longitude,l.accuracy_meters,l.heading_degrees,l.speed_mps,l.recorded_at,l.received_at,
        case when l.recorded_at is null then true else l.recorded_at < now() - interval '2 minutes' end location_stale
       from public.orders o
       left join public.delivery_assignments a on a.order_id=o.id
       left join lateral (
         select latitude,longitude,accuracy_meters,heading_degrees,speed_mps,recorded_at,received_at
         from public.partner_location_updates where assignment_id=a.id order by recorded_at desc limit 1
       ) l on true
       where o.id=$1 and o.customer_user_id=$2`,
      [orderId, userId],
    );
    if (!result.rowCount) throw new NotFoundException('Order tracking is unavailable');
    return result.rows[0];
  }

  async adminActiveDeliveries() {
    const result = await this.db.query(
      `select o.id order_id,o.status order_status,o.delivery_address,a.id assignment_id,a.status delivery_status,
        p.display_name partner_name,l.latitude,l.longitude,l.accuracy_meters,l.recorded_at,l.received_at,
        case when l.recorded_at is null then true else l.recorded_at < now() - interval '2 minutes' end location_stale
       from public.delivery_assignments a
       join public.orders o on o.id=a.order_id
       join public.delivery_partner_profiles p on p.user_id=a.partner_user_id
       left join lateral (
         select latitude,longitude,accuracy_meters,recorded_at,received_at
         from public.partner_location_updates where assignment_id=a.id order by recorded_at desc limit 1
       ) l on true
       where a.status in ('assigned','accepted','arrived_pickup','picked_up','out_for_delivery')
       order by a.updated_at desc limit 200`,
    );
    return { items: result.rows };
  }
}
