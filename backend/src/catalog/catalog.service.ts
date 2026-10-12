import { Injectable, Logger, ServiceUnavailableException } from '@nestjs/common';
import { DatabaseService } from '../database.service';
import { classifyDatabaseFailure } from '../database.config';
import { CatalogQueryDto } from './dto';

@Injectable()
export class CatalogService {
  private readonly logger = new Logger(CatalogService.name);

  constructor(private readonly db: DatabaseService) {}

  private async query(text: string, values: unknown[] = []) {
    try {
      return await this.db.query(text, values);
    } catch (error) {
      const code = classifyDatabaseFailure(error);
      this.logger.error(`Catalog database query failed (${code})`);
      throw new ServiceUnavailableException({
        status: 'error',
        code: 'CATALOG_TEMPORARILY_UNAVAILABLE',
      });
    }
  }

  async products(query: CatalogQueryDto) {
    const result = await this.query(
      `select p.id::text,p.name,p.description,p.unit,p.package_size,p.price,p.compare_at_price,
        p.image_path,c.id::text category_id,c.name category_name,
        greatest(coalesce(i.on_hand,0)-coalesce(i.reserved,0),0)::int available_quantity
       from public.products p
       left join public.categories c on c.id=p.category_id
       left join public.inventory i on i.product_id=p.id
       where p.is_visible=true and p.is_archived=false
         and (c.id is null or c.is_visible=true)
         and ($1::bigint is null or p.category_id=$1)
         and ($2::text is null or p.name ilike '%'||$2||'%' or p.description ilike '%'||$2||'%')
       order by p.name,p.id
       limit $3`,
      [query.categoryId ?? null, query.search?.trim() || null, query.limit],
    );
    return { items: result.rows };
  }

  async categories() {
    const result = await this.query(
      `select c.id::text,c.name,c.slug,c.description,c.image_path,
        count(p.id) filter (where p.is_visible=true and p.is_archived=false)::int product_count
       from public.categories c
       left join public.products p on p.category_id=c.id
       where c.is_visible=true
       group by c.id
       order by c.sort_order,c.name`,
    );
    return { items: result.rows };
  }

  async promotions() {
    const result = await this.query(
      `select id::text,title,description,kind,festival_name,code,discount_type,discount_value,
        minimum_order_value,maximum_discount,expires_at,banner_path,terms,first_order_only
       from public.promotions
       where is_enabled=true and starts_at<=now() and expires_at>now()
         and (usage_limit is null or usage_count<usage_limit)
       order by expires_at,id
       limit 50`,
    );
    return { items: result.rows };
  }
}
