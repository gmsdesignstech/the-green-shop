import { ConflictException, Injectable, NotFoundException, UnprocessableEntityException } from '@nestjs/common';
import { DatabaseService } from '../database.service';
import { AssignPartnerDto, CategoryDto, InventoryAdjustmentDto, OrderStatusDto, PageQueryDto, ProductDto, PromotionDto } from './dto';

const transitions: Record<string, string[]> = {
  placed: ['confirmed', 'cancelled'], confirmed: ['preparing', 'cancelled'],
  preparing: ['rider_assigned', 'cancelled'], rider_assigned: ['out_for_delivery', 'cancelled'],
  out_for_delivery: ['delivered'], delivered: [], cancelled: [],
};

@Injectable()
export class AdminService {
  constructor(private readonly db: DatabaseService) {}

  async dashboard(from?: string, to?: string) {
    const range = this.range(from, to);
    const [metrics, sales, recent, best, low, activity] = await Promise.all([
      this.db.query(`select count(*) filter (where created_at::date=current_date)::int today_orders,
        count(*) filter (where status in ('placed','confirmed','preparing'))::int pending_orders,
        count(*) filter (where status='delivered')::int completed_orders,
        coalesce(sum(total) filter (where payment_status='successful'),0) gross_sales,
        count(*) filter (where payment_status='failed')::int payment_failures,
        count(*) filter (where status in ('rider_assigned','out_for_delivery'))::int active_deliveries
        from public.orders where created_at >= $1 and created_at < $2`, range),
      this.db.query(`select date_trunc('day',created_at)::date day, coalesce(sum(total),0) gross_sales
        from public.orders where payment_status='successful' and created_at >= $1 and created_at < $2
        group by 1 order by 1`, range),
      this.db.query(`select id,status,payment_status,total,created_at from public.orders order by created_at desc limit 8`),
      this.db.query(`select oi.product_id,oi.product_name,sum(oi.quantity)::int quantity
        from public.order_items oi join public.orders o on o.id=oi.order_id where o.status<>'cancelled' and o.created_at >= $1 and o.created_at < $2
        group by oi.product_id,oi.product_name order by quantity desc limit 5`, range),
      this.db.query(`select p.id,p.name,i.on_hand,i.reserved,(i.on_hand-i.reserved) available,i.low_stock_threshold
        from public.inventory i join public.products p on p.id=i.product_id
        where not p.is_archived and i.on_hand-i.reserved <= i.low_stock_threshold order by available asc limit 10`),
      this.db.query(`select action,target_type,target_id,created_at from public.audit_logs order by created_at desc limit 8`),
    ]);
    const pending = await this.db.query("select count(*)::int count from public.delivery_partner_applications where status in ('pending','under_review')");
    return { metrics: { ...metrics.rows[0], low_stock_products: low.rowCount, pending_partner_applications: pending.rows[0].count }, sales: sales.rows, recentOrders: recent.rows, bestSellingProducts: best.rows, lowStock: low.rows, recentActivity: activity.rows };
  }

  async products(q: PageQueryDto) {
    const cursor = this.cursor(q.cursor);
    const result = await this.db.query(`select p.*,c.name category_name,i.on_hand,i.reserved,i.low_stock_threshold
      from public.products p left join public.categories c on c.id=p.category_id left join public.inventory i on i.product_id=p.id
      where p.id < $1 and ($2::text is null or p.name ilike '%'||$2||'%' or p.sku ilike '%'||$2||'%')
      order by p.id desc limit $3`, [cursor, q.search ?? null, q.limit + 1]);
    return this.page(result.rows, q.limit);
  }

  async createProduct(actor: string, dto: ProductDto) {
    if (dto.compareAtPrice != null && dto.compareAtPrice < dto.price) throw new UnprocessableEntityException('Compare-at price must be at least the price');
    return this.db.transaction(async client => {
      const product = await client.query(`insert into public.products(category_id,sku,name,description,unit,package_size,price,compare_at_price,image_path,is_visible)
        values($1,$2,$3,$4,$5,$6,$7,$8,$9,$10) returning *`, [dto.categoryId ?? null,dto.sku?.trim() || null,dto.name.trim(),dto.description.trim(),dto.unit.trim(),dto.packageSize?.trim() || null,dto.price,dto.compareAtPrice ?? null,dto.imagePath ?? null,dto.isVisible]);
      await client.query('insert into public.inventory(product_id) values($1)', [product.rows[0].id]);
      await this.audit(client, actor, 'product.created', 'product', product.rows[0].id, {});
      return product.rows[0];
    });
  }

  async updateProduct(actor: string, id: number, dto: ProductDto) {
    if (dto.compareAtPrice != null && dto.compareAtPrice < dto.price) throw new UnprocessableEntityException('Compare-at price must be at least the price');
    return this.db.transaction(async client => {
      const before = await client.query('select price from public.products where id=$1 for update', [id]);
      if (!before.rowCount) throw new NotFoundException('Product not found');
      const result = await client.query(`update public.products set category_id=$1,sku=$2,name=$3,description=$4,unit=$5,package_size=$6,price=$7,compare_at_price=$8,image_path=$9,is_visible=$10,updated_at=now() where id=$11 returning *`, [dto.categoryId ?? null,dto.sku?.trim() || null,dto.name.trim(),dto.description.trim(),dto.unit.trim(),dto.packageSize?.trim() || null,dto.price,dto.compareAtPrice ?? null,dto.imagePath ?? null,dto.isVisible,id]);
      await this.audit(client, actor, before.rows[0].price === String(dto.price) ? 'product.updated' : 'product.price_changed', 'product', id, { previousPrice: before.rows[0].price, newPrice: dto.price });
      return result.rows[0];
    });
  }

  async archiveProduct(actor: string, id: number, archived: boolean) {
    const result = await this.db.query('update public.products set is_archived=$1,is_visible=case when $1 then false else is_visible end,updated_at=now() where id=$2 returning *', [archived,id]);
    if (!result.rowCount) throw new NotFoundException('Product not found');
    await this.db.query('insert into public.audit_logs(actor_user_id,action,target_type,target_id) values($1,$2,$3,$4)', [actor, archived ? 'product.archived' : 'product.restored','product',String(id)]);
    return result.rows[0];
  }

  async categories(q: PageQueryDto) {
    const cursor=this.cursor(q.cursor); const result=await this.db.query(`select c.*,
      count(p.id) filter (where not p.is_archived)::int product_count from public.categories c
      left join public.products p on p.category_id=c.id where c.id<$1 and ($2::text is null or c.name ilike '%'||$2||'%')
      group by c.id order by c.sort_order,c.id desc limit $3`,[cursor,q.search??null,q.limit+1]);
    return this.page(result.rows,q.limit);
  }

  async createCategory(actor:string,dto:CategoryDto){
    try { const result=await this.db.query(`insert into public.categories(name,slug,description,image_path,sort_order,is_visible)
      values($1,lower($2),$3,$4,$5,$6) returning *`,[dto.name.trim(),dto.slug.trim(),dto.description?.trim()??'',dto.imagePath?.trim()||null,dto.sortOrder,dto.isVisible]);
      await this.db.query('insert into public.audit_logs(actor_user_id,action,target_type,target_id) values($1,$2,$3,$4)',[actor,'category.created','category',String(result.rows[0].id)]); return result.rows[0];
    } catch(error:any){if(error?.code==='23505') throw new ConflictException('Category slug already exists'); throw error;}
  }

  async updateCategory(actor:string,id:number,dto:CategoryDto){
    const result=await this.db.query(`update public.categories set name=$1,slug=lower($2),description=$3,image_path=$4,sort_order=$5,is_visible=$6,updated_at=now() where id=$7 returning *`,[dto.name.trim(),dto.slug.trim(),dto.description?.trim()??'',dto.imagePath?.trim()||null,dto.sortOrder,dto.isVisible,id]);
    if(!result.rowCount) throw new NotFoundException('Category not found');
    await this.db.query('insert into public.audit_logs(actor_user_id,action,target_type,target_id) values($1,$2,$3,$4)',[actor,'category.updated','category',String(id)]); return result.rows[0];
  }

  async promotions(q:PageQueryDto){const cursor=this.cursor(q.cursor);const result=await this.db.query(`select *,
    case when not is_enabled then 'paused' when now()<starts_at then 'scheduled' when now()>=expires_at then 'expired' else 'active' end status
    from public.promotions where id<$1 and ($2::text is null or title ilike '%'||$2||'%' or code ilike '%'||$2||'%')
    and ($3::text is null or kind=$3) order by id desc limit $4`,[cursor,q.search??null,q.status??null,q.limit+1]);return this.page(result.rows,q.limit);}

  createPromotion(actor:string,dto:PromotionDto){return this.savePromotion(actor,null,dto);}
  updatePromotion(actor:string,id:number,dto:PromotionDto){return this.savePromotion(actor,id,dto);}

  private async savePromotion(actor:string,id:number|null,dto:PromotionDto){
    const start=new Date(dto.startsAt),end=new Date(dto.expiresAt);
    if(Number.isNaN(start.valueOf())||Number.isNaN(end.valueOf())||start>=end) throw new UnprocessableEntityException('Promotion expiry must be after its start time');
    if(dto.discountType==='percentage'&&dto.discountValue>100) throw new UnprocessableEntityException('Percentage discount cannot exceed 100');
    const code=dto.code?.trim().toUpperCase()||null;
    try{return await this.db.transaction(async client=>{const values=[dto.title.trim(),dto.description.trim(),dto.kind,dto.festivalName?.trim()||null,code,dto.discountType,dto.discountValue,dto.minimumOrderValue,dto.maximumDiscount??null,start,end,dto.usageLimit??null,dto.perCustomerLimit,dto.firstOrderOnly,dto.bannerPath?.trim()||null,dto.terms.trim(),dto.isEnabled];
      const result=id==null?await client.query(`insert into public.promotions(title,description,kind,festival_name,code,discount_type,discount_value,minimum_order_value,maximum_discount,starts_at,expires_at,usage_limit,per_customer_limit,first_order_only,banner_path,terms,is_enabled)
      values($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17) returning *`,values):await client.query(`update public.promotions set title=$1,description=$2,kind=$3,festival_name=$4,code=$5,discount_type=$6,discount_value=$7,minimum_order_value=$8,maximum_discount=$9,starts_at=$10,expires_at=$11,usage_limit=$12,per_customer_limit=$13,first_order_only=$14,banner_path=$15,terms=$16,is_enabled=$17,updated_at=now() where id=$18 returning *`,[...values,id]);
      if(!result.rowCount) throw new NotFoundException('Promotion not found');await this.audit(client,actor,id==null?'promotion.created':'promotion.updated','promotion',result.rows[0].id,{kind:dto.kind});return result.rows[0];});
    }catch(error:any){if(error?.code==='23505') throw new ConflictException('Coupon code already exists');throw error;}}

  async inventory(q: PageQueryDto) {
    const cursor = this.cursor(q.cursor);
    const result = await this.db.query(`select p.id product_id,p.name,i.on_hand,i.reserved,(i.on_hand-i.reserved) available,i.low_stock_threshold,i.updated_at
      from public.inventory i join public.products p on p.id=i.product_id where p.id < $1 and not p.is_archived
      and ($2::text is null or p.name ilike '%'||$2||'%') order by p.id desc limit $3`, [cursor,q.search ?? null,q.limit+1]);
    return this.page(result.rows,q.limit,'product_id');
  }

  adjustInventory(actor: string, productId: number, dto: InventoryAdjustmentDto) {
    return this.db.transaction(async client => {
      const result = await client.query(`update public.inventory set on_hand=on_hand+$1,
        low_stock_threshold=coalesce($2,low_stock_threshold),updated_at=now()
        where product_id=$3 and on_hand+$1 >= reserved returning *`, [dto.delta,dto.lowStockThreshold ?? null,productId]);
      if (!result.rowCount) throw new ConflictException('Adjustment would make available inventory negative, or product was not found');
      await client.query('insert into public.inventory_movements(product_id,delta,reason,actor_user_id,resulting_on_hand) values($1,$2,$3,$4,$5)', [productId,dto.delta,dto.reason.trim(),actor,result.rows[0].on_hand]);
      await this.audit(client,actor,'inventory.adjusted','product',productId,{delta:dto.delta,reason:dto.reason});
      return result.rows[0];
    });
  }

  async orders(q: PageQueryDto) {
    const cursor=this.cursor(q.cursor); const range=this.range(q.from,q.to);
    const result=await this.db.query(`select id,status,payment_status,subtotal,discount_total,delivery_fee,total,assigned_partner_user_id,version,created_at
      from public.orders where id<$1 and ($2::text is null or status=$2) and ($3::text is null or payment_status=$3)
      and created_at >= $4 and created_at < $5 and ($6::text is null or id::text=$6)
      order by id desc limit $7`,[cursor,q.status??null,q.paymentStatus??null,...range,q.search??null,q.limit+1]);
    return this.page(result.rows,q.limit);
  }

  async orderDetail(id:number) {
    const order=await this.db.query('select * from public.orders where id=$1',[id]);
    if(!order.rowCount) throw new NotFoundException('Order not found');
    const [items,history,payments]=await Promise.all([this.db.query('select * from public.order_items where order_id=$1 order by id',[id]),this.db.query('select * from public.order_status_history where order_id=$1 order by created_at',[id]),this.db.query('select id,provider,method,status,transaction_reference,amount,refunded_amount,created_at from public.payments where order_id=$1 order by created_at desc',[id])]);
    return {...order.rows[0],items:items.rows,history:history.rows,payments:payments.rows};
  }

  changeOrderStatus(actor:string,id:number,dto:OrderStatusDto){ return this.db.transaction(async client=>{
    const found=await client.query('select status,version from public.orders where id=$1 for update',[id]);
    if(!found.rowCount) throw new NotFoundException('Order not found');
    if(found.rows[0].version!==dto.version) throw new ConflictException('Order changed; refresh and retry');
    if(!transitions[found.rows[0].status]?.includes(dto.status)) throw new UnprocessableEntityException(`Invalid transition from ${found.rows[0].status} to ${dto.status}`);
    if(dto.status==='cancelled'&&!dto.reason?.trim()) throw new UnprocessableEntityException('Cancellation reason is required');
    const updated=await client.query('update public.orders set status=$1,cancellation_reason=case when $1=\'cancelled\' then $2 else cancellation_reason end,version=version+1,updated_at=now() where id=$3 returning *',[dto.status,dto.reason?.trim()??null,id]);
    await client.query('insert into public.order_status_history(order_id,from_status,to_status,actor_user_id,note) values($1,$2,$3,$4,$5)',[id,found.rows[0].status,dto.status,actor,dto.reason??null]);
    await this.audit(client,actor,`order.${dto.status}`,'order',id,{from:found.rows[0].status,reason:dto.reason??null}); return updated.rows[0];
  });}

  assignPartner(actor:string,id:number,dto:AssignPartnerDto){return this.db.transaction(async client=>{
    const eligible=await client.query("select 1 from public.trusted_role_assignments where user_id=$1 and role='delivery_partner' and status='active'",[dto.partnerUserId]);
    if(!eligible.rowCount) throw new UnprocessableEntityException('Partner is not active and eligible');
    const updated=await client.query("update public.orders set assigned_partner_user_id=$1,status='rider_assigned',version=version+1,updated_at=now() where id=$2 and version=$3 and status in ('confirmed','preparing','rider_assigned') returning *",[dto.partnerUserId,id,dto.version]);
    if(!updated.rowCount) throw new ConflictException('Order cannot be assigned in its current state; refresh and retry');
    await client.query(`insert into public.delivery_assignments(order_id,partner_user_id,status)
      values($1,$2,'assigned') on conflict(order_id) do update set partner_user_id=excluded.partner_user_id,
      status='assigned',version=public.delivery_assignments.version+1,updated_at=now()`,[id,dto.partnerUserId]);
    await client.query(`insert into public.partner_notifications(partner_user_id,type,title,message,assignment_id)
      select $1,'assignment','New delivery assigned','Open the app to review your assigned delivery',id
      from public.delivery_assignments where order_id=$2`,[dto.partnerUserId,id]);
    await this.audit(client,actor,'order.partner_assigned','order',id,{partnerUserId:dto.partnerUserId}); return updated.rows[0];
  });}

  async payments(q:PageQueryDto){const cursor=this.cursor(q.cursor);const result=await this.db.query(`select p.id,p.order_id,p.provider,p.method,p.status,p.transaction_reference,p.amount,p.refunded_amount,p.created_at from public.payments p where p.id<$1 and ($2::text is null or p.status=$2) order by p.id desc limit $3`,[cursor,q.status??null,q.limit+1]);return this.page(result.rows,q.limit);}
  async notifications(q:PageQueryDto){const cursor=this.cursor(q.cursor);const result=await this.db.query('select * from public.admin_notifications where id<$1 order by id desc limit $2',[cursor,q.limit+1]);return this.page(result.rows,q.limit);}
  async auditLogs(q:PageQueryDto){const cursor=this.cursor(q.cursor);const result=await this.db.query('select id,actor_user_id,action,target_type,target_id,metadata,created_at from public.audit_logs where id<$1 order by id desc limit $2',[cursor,q.limit+1]);return this.page(result.rows,q.limit);}
  async support(q:PageQueryDto){const cursor=this.cursor(q.cursor);const result=await this.db.query(`select id,category,priority,status,created_at from public.support_tickets where id<$1 and ($2::text is null or status=$2) order by id desc limit $3`,[cursor,q.status??null,q.limit+1]);return this.page(result.rows,q.limit);}
  async storeSettings(){const result=await this.db.query('select store_name,contact_phone,contact_email,operating_hours,delivery_enabled,delivery_fee,minimum_order_value,supported_payment_methods,updated_at from public.store_settings where id=true');return result.rows[0]??null;}
  async reports(from?:string,to?:string){const range=this.range(from,to);const result=await this.db.query(`select count(*)::int orders,coalesce(sum(total),0) gross_sales,coalesce(sum(discount_total),0) discounts,coalesce(sum(delivery_fee),0) delivery_fees,count(*) filter(where status='cancelled')::int cancellations,count(*) filter(where status='delivered')::int deliveries,count(*) filter(where payment_status='failed')::int payment_failures from public.orders where created_at >= $1 and created_at < $2`,range);return result.rows[0];}

  private cursor(value?:string){const parsed=value?Number(value):Number.MAX_SAFE_INTEGER;if(!Number.isSafeInteger(parsed)||parsed<1) throw new UnprocessableEntityException('Invalid cursor');return parsed;}
  private range(from?:string,to?:string):[Date,Date]{const end=to?new Date(to):new Date();const start=from?new Date(from):new Date(end.getTime()-7*86400000);if(Number.isNaN(start.valueOf())||Number.isNaN(end.valueOf())||start>=end) throw new UnprocessableEntityException('Invalid date range');return[start,end];}
  private page(rows:any[],limit:number,key='id'){const hasMore=rows.length>limit;const items=hasMore?rows.slice(0,limit):rows;return{items,nextCursor:hasMore?String(items[items.length-1][key]):null};}
  private audit(client:any,actor:string,action:string,type:string,id:any,metadata:any){return client.query('insert into public.audit_logs(actor_user_id,action,target_type,target_id,metadata) values($1,$2,$3,$4,$5::jsonb)',[actor,action,type,String(id),JSON.stringify(metadata)]);}
}
