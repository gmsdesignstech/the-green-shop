import { Injectable, NotFoundException, UnprocessableEntityException } from '@nestjs/common';
import { DatabaseService } from '../database.service';
import { CustomerAddressDto, CustomerOrdersQueryDto } from './dto';

@Injectable()
export class CustomerService {
  constructor(private readonly db: DatabaseService) {}

  async orders(userId: string, query: CustomerOrdersQueryDto) {
    const cursor = query.cursor ?? Number.MAX_SAFE_INTEGER;
    const result = await this.db.query(
      `select o.id::text,o.status,o.payment_status,o.subtotal,o.discount_total,o.delivery_fee,o.total,
        o.delivery_address,o.created_at,o.updated_at,
        coalesce(jsonb_agg(jsonb_build_object(
          'product_id',oi.product_id::text,'name',oi.product_name,'unit_price',oi.unit_price,
          'quantity',oi.quantity,'line_total',oi.line_total,'unit',p.unit,'package_size',p.package_size,
          'image_path',p.image_path,'category_id',p.category_id::text
        ) order by oi.id) filter (where oi.id is not null),'[]'::jsonb) items
       from public.orders o
       left join public.order_items oi on oi.order_id=o.id
       left join public.products p on p.id=oi.product_id
       where o.customer_user_id=$1 and o.id<$2
       group by o.id
       order by o.id desc
       limit $3`,
      [userId, cursor, query.limit + 1],
    );
    const hasMore = result.rows.length > query.limit;
    const items = result.rows.slice(0, query.limit);
    return { items, nextCursor: hasMore ? String(items.at(-1)?.id) : null };
  }

  async order(userId: string, orderId: number) {
    const result = await this.db.query(
      `select o.id::text,o.status,o.payment_status,o.subtotal,o.discount_total,o.delivery_fee,o.total,
        o.delivery_address,o.created_at,o.updated_at,
        coalesce(jsonb_agg(jsonb_build_object(
          'product_id',oi.product_id::text,'name',oi.product_name,'unit_price',oi.unit_price,
          'quantity',oi.quantity,'line_total',oi.line_total,'unit',p.unit,'package_size',p.package_size,
          'image_path',p.image_path,'category_id',p.category_id::text
        ) order by oi.id) filter (where oi.id is not null),'[]'::jsonb) items
       from public.orders o
       left join public.order_items oi on oi.order_id=o.id
       left join public.products p on p.id=oi.product_id
       where o.id=$1 and o.customer_user_id=$2
       group by o.id`,
      [orderId, userId],
    );
    if (!result.rowCount) throw new NotFoundException('Order not found');
    return result.rows[0];
  }

  async addresses(userId: string) {
    const result = await this.db.query(
      `select id::text,label,address_line,locality,postal_code,latitude,longitude,is_default,created_at,updated_at
       from public.customer_addresses where user_id=$1 order by is_default desc,created_at desc,id desc`,
      [userId],
    );
    return { items: result.rows };
  }

  createAddress(userId: string, dto: CustomerAddressDto) {
    this.coordinates(dto);
    return this.db.transaction(async client => {
      await client.query('select pg_advisory_xact_lock(hashtext($1))', [userId]);
      if (dto.isDefault) await client.query('update public.customer_addresses set is_default=false,updated_at=now() where user_id=$1 and is_default', [userId]);
      const result = await client.query(
        `insert into public.customer_addresses(user_id,label,address_line,locality,postal_code,latitude,longitude,is_default)
         values($1,$2,$3,$4,$5,$6,$7,$8 or not exists(select 1 from public.customer_addresses where user_id=$1))
         returning id::text,label,address_line,locality,postal_code,latitude,longitude,is_default,created_at,updated_at`,
        [userId,dto.label.trim(),dto.addressLine.trim(),dto.locality.trim(),dto.postalCode??null,dto.latitude??null,dto.longitude??null,dto.isDefault],
      );
      return result.rows[0];
    });
  }

  updateAddress(userId: string, addressId: number, dto: CustomerAddressDto) {
    this.coordinates(dto);
    return this.db.transaction(async client => {
      await client.query('select pg_advisory_xact_lock(hashtext($1))', [userId]);
      if (dto.isDefault) await client.query('update public.customer_addresses set is_default=false,updated_at=now() where user_id=$1 and id<>$2 and is_default', [userId,addressId]);
      const result = await client.query(
        `update public.customer_addresses set label=$1,address_line=$2,locality=$3,postal_code=$4,
          latitude=$5,longitude=$6,is_default=$7,updated_at=now()
         where id=$8 and user_id=$9
         returning id::text,label,address_line,locality,postal_code,latitude,longitude,is_default,created_at,updated_at`,
        [dto.label.trim(),dto.addressLine.trim(),dto.locality.trim(),dto.postalCode??null,dto.latitude??null,dto.longitude??null,dto.isDefault,addressId,userId],
      );
      if (!result.rowCount) throw new NotFoundException('Address not found');
      return result.rows[0];
    });
  }

  deleteAddress(userId: string, addressId: number) {
    return this.db.transaction(async client => {
      await client.query('select pg_advisory_xact_lock(hashtext($1))', [userId]);
      const removed = await client.query('delete from public.customer_addresses where id=$1 and user_id=$2 returning is_default', [addressId,userId]);
      if (!removed.rowCount) throw new NotFoundException('Address not found');
      if (removed.rows[0].is_default) {
        await client.query(`update public.customer_addresses set is_default=true,updated_at=now()
          where id=(select id from public.customer_addresses where user_id=$1 order by created_at desc,id desc limit 1)`, [userId]);
      }
      return { deleted: true };
    });
  }

  private coordinates(dto: CustomerAddressDto) {
    if ((dto.latitude == null) !== (dto.longitude == null)) {
      throw new UnprocessableEntityException('Latitude and longitude must be supplied together');
    }
  }
}
