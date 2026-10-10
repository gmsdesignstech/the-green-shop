-- Authoritative commerce and admin operations. This project uses declarative schemas;
-- generate a forward-only migration from this desired state before deployment.

create table if not exists public.categories (
  id bigint generated always as identity primary key,
  name text not null check (char_length(name) between 2 and 120),
  slug text not null unique check (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
  description text not null default '' check (char_length(description) <= 500),
  image_path text,
  sort_order integer not null default 0,
  is_visible boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.products (
  id bigint generated always as identity primary key,
  category_id bigint references public.categories(id) on delete restrict,
  sku text unique,
  name text not null check (char_length(name) between 2 and 160),
  description text not null default '' check (char_length(description) <= 4000),
  unit text not null check (char_length(unit) between 1 and 40),
  package_size text check (package_size is null or char_length(package_size) <= 80),
  price numeric(12,2) not null check (price >= 0),
  compare_at_price numeric(12,2) check (compare_at_price is null or compare_at_price >= price),
  image_path text,
  is_visible boolean not null default true,
  is_archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists products_category_idx on public.products(category_id) where is_archived = false;
create index if not exists products_active_name_idx on public.products(lower(name)) where is_archived = false;

create table if not exists public.inventory (
  product_id bigint primary key references public.products(id) on delete restrict,
  on_hand integer not null default 0 check (on_hand >= 0),
  reserved integer not null default 0 check (reserved >= 0 and reserved <= on_hand),
  low_stock_threshold integer not null default 5 check (low_stock_threshold >= 0),
  updated_at timestamptz not null default now()
);
create index if not exists inventory_low_stock_idx on public.inventory(on_hand, low_stock_threshold);

create table if not exists public.inventory_movements (
  id bigint generated always as identity primary key,
  product_id bigint not null references public.products(id) on delete restrict,
  delta integer not null check (delta <> 0),
  reason text not null check (char_length(reason) between 3 and 500),
  actor_user_id uuid references auth.users(id) on delete restrict,
  resulting_on_hand integer not null check (resulting_on_hand >= 0),
  created_at timestamptz not null default now()
);
create index if not exists inventory_movements_product_created_idx on public.inventory_movements(product_id, created_at desc);
create index if not exists inventory_movements_actor_idx on public.inventory_movements(actor_user_id) where actor_user_id is not null;

create table if not exists public.orders (
  id bigint generated always as identity primary key,
  customer_user_id uuid not null references auth.users(id) on delete restrict,
  status text not null default 'pending_payment' check (status in ('pending_payment','placed','confirmed','preparing','rider_assigned','out_for_delivery','delivered','cancelled','refunded')),
  payment_status text not null default 'created' check (payment_status in ('created','pending','authorized','captured','failed','cancelled','refund_pending','refunded','partially_refunded','cod_pending','cod_collected')),
  subtotal numeric(12,2) not null check (subtotal >= 0),
  discount_total numeric(12,2) not null default 0 check (discount_total >= 0),
  delivery_fee numeric(12,2) not null default 0 check (delivery_fee >= 0),
  total numeric(12,2) not null check (total >= 0 and total = subtotal - discount_total + delivery_fee),
  delivery_address jsonb not null default '{}'::jsonb,
  assigned_partner_user_id uuid references auth.users(id) on delete restrict,
  cancellation_reason text,
  version integer not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists orders_customer_created_idx on public.orders(customer_user_id, created_at desc);
create index if not exists orders_status_created_idx on public.orders(status, created_at desc);
create index if not exists orders_payment_created_idx on public.orders(payment_status, created_at desc);
create index if not exists orders_partner_active_idx on public.orders(assigned_partner_user_id, created_at desc) where assigned_partner_user_id is not null;

create table if not exists public.order_items (
  id bigint generated always as identity primary key,
  order_id bigint not null references public.orders(id) on delete restrict,
  product_id bigint not null references public.products(id) on delete restrict,
  product_name text not null,
  unit_price numeric(12,2) not null check (unit_price >= 0),
  quantity integer not null check (quantity > 0),
  line_total numeric(12,2) not null check (line_total >= 0),
  unique (order_id, product_id)
);
create index if not exists order_items_product_idx on public.order_items(product_id);

create table if not exists public.order_status_history (
  id bigint generated always as identity primary key,
  order_id bigint not null references public.orders(id) on delete restrict,
  from_status text,
  to_status text not null,
  actor_user_id uuid references auth.users(id) on delete restrict,
  note text,
  created_at timestamptz not null default now()
);
create index if not exists order_status_history_order_created_idx on public.order_status_history(order_id, created_at);

create table if not exists public.payments (
  id bigint generated always as identity primary key,
  order_id bigint not null references public.orders(id) on delete restrict,
  provider text not null,
  method text not null,
  status text not null check (status in ('created','pending','authorized','captured','failed','cancelled','refund_pending','refunded','partially_refunded')),
  transaction_reference text,
  amount numeric(12,2) not null check (amount >= 0),
  refunded_amount numeric(12,2) not null default 0 check (refunded_amount >= 0 and refunded_amount <= amount),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(provider, transaction_reference)
);
create index if not exists payments_order_idx on public.payments(order_id);
create index if not exists payments_status_created_idx on public.payments(status, created_at desc);

create table if not exists public.payment_attempts (
  id uuid primary key default gen_random_uuid(),
  order_id bigint not null references public.orders(id) on delete restrict,
  customer_user_id uuid not null references auth.users(id) on delete restrict,
  provider text not null check (provider in ('razorpay','cod')),
  idempotency_key text not null,
  gateway_order_id text unique,
  gateway_payment_id text unique,
  status text not null default 'created' check (status in ('created','pending','authorized','captured','failed','cancelled')),
  amount_minor bigint not null check (amount_minor > 0),
  currency text not null default 'INR' check (currency ~ '^[A-Z]{3}$'),
  failure_code text,
  failure_description text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(customer_user_id,idempotency_key)
);
create index if not exists payment_attempts_order_idx on public.payment_attempts(order_id,created_at desc);
create index if not exists payment_attempts_pending_idx on public.payment_attempts(created_at) where status in ('created','pending','authorized');

create table if not exists public.payment_webhook_events (
  id bigint generated always as identity primary key,
  provider text not null,
  event_id text not null,
  event_type text not null,
  payload_sha256 text not null,
  processed_at timestamptz,
  processing_error text,
  received_at timestamptz not null default now(),
  unique(provider,event_id)
);

create table if not exists public.payment_refunds (
  id bigint generated always as identity primary key,
  payment_id bigint not null references public.payments(id) on delete restrict,
  provider_refund_id text unique,
  idempotency_key text not null unique,
  amount numeric(12,2) not null check (amount > 0),
  status text not null check (status in ('pending','processed','failed')),
  requested_by uuid references auth.users(id) on delete restrict,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.cod_collections (
  id bigint generated always as identity primary key,
  order_id bigint not null unique references public.orders(id) on delete restrict,
  partner_user_id uuid not null references auth.users(id) on delete restrict,
  expected_amount numeric(12,2) not null check (expected_amount > 0),
  collected_amount numeric(12,2) check (collected_amount is null or collected_amount >= 0),
  status text not null default 'expected' check (status in ('expected','collected','reconciled','disputed')),
  collected_at timestamptz,
  reconciled_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.promotions (
  id bigint generated always as identity primary key,
  title text not null check (char_length(title) between 2 and 160),
  description text not null default '' check (char_length(description) <= 4000),
  kind text not null default 'coupon' check (kind in ('daily','festival','coupon')),
  festival_name text check (festival_name is null or char_length(festival_name) <= 80),
  code text unique check (code is null or (code = upper(code) and char_length(code) between 3 and 40)),
  discount_type text not null check (discount_type in ('fixed','percentage')),
  discount_value numeric(12,2) not null check (discount_value > 0),
  minimum_order_value numeric(12,2) not null default 0 check (minimum_order_value >= 0),
  maximum_discount numeric(12,2) check (maximum_discount is null or maximum_discount > 0),
  starts_at timestamptz not null,
  expires_at timestamptz not null check (expires_at > starts_at),
  usage_limit integer check (usage_limit is null or usage_limit > 0),
  usage_count integer not null default 0 check (usage_count >= 0),
  per_customer_limit integer not null default 1 check (per_customer_limit > 0),
  first_order_only boolean not null default false,
  banner_path text,
  terms text not null default '' check (char_length(terms) <= 4000),
  is_enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (discount_type <> 'percentage' or discount_value <= 100)
);
create index if not exists promotions_active_idx on public.promotions(starts_at, expires_at) where is_enabled = true;
create index if not exists promotions_kind_active_idx on public.promotions(kind, starts_at, expires_at) where is_enabled = true;
create unique index if not exists promotions_code_unique_idx on public.promotions(code) where code is not null;

create table if not exists public.promotion_products (
  promotion_id bigint not null references public.promotions(id) on delete cascade,
  product_id bigint not null references public.products(id) on delete restrict,
  primary key (promotion_id, product_id)
);
create index if not exists promotion_products_product_idx on public.promotion_products(product_id);

create table if not exists public.promotion_categories (
  promotion_id bigint not null references public.promotions(id) on delete cascade,
  category_id bigint not null references public.categories(id) on delete restrict,
  primary key (promotion_id, category_id)
);
create index if not exists promotion_categories_category_idx on public.promotion_categories(category_id);

create table if not exists public.promotion_redemptions (
  id bigint generated always as identity primary key,
  promotion_id bigint not null references public.promotions(id) on delete restrict,
  customer_user_id uuid not null references auth.users(id) on delete restrict,
  order_id bigint not null references public.orders(id) on delete restrict,
  discount_amount numeric(12,2) not null check (discount_amount > 0),
  created_at timestamptz not null default now(),
  unique (promotion_id, order_id)
);
create index if not exists promotion_redemptions_customer_idx on public.promotion_redemptions(promotion_id, customer_user_id, created_at desc);

create table if not exists public.admin_notifications (
  id bigint generated always as identity primary key,
  type text not null,
  title text not null,
  message text not null,
  resource_type text,
  resource_id text,
  read_at timestamptz,
  created_at timestamptz not null default now()
);
create index if not exists admin_notifications_unread_idx on public.admin_notifications(created_at desc) where read_at is null;

create table if not exists public.support_tickets (
  id bigint generated always as identity primary key,
  customer_user_id uuid not null references auth.users(id) on delete restrict,
  order_id bigint references public.orders(id) on delete restrict,
  category text not null,
  description text not null,
  priority text not null default 'normal' check (priority in ('low','normal','high','urgent')),
  status text not null default 'open' check (status in ('open','in_progress','resolved','closed')),
  assigned_user_id uuid references auth.users(id) on delete restrict,
  internal_notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists support_tickets_customer_idx on public.support_tickets(customer_user_id, created_at desc);
create index if not exists support_tickets_status_priority_idx on public.support_tickets(status, priority, created_at);
create index if not exists support_tickets_order_idx on public.support_tickets(order_id) where order_id is not null;
create index if not exists support_tickets_assignee_idx on public.support_tickets(assigned_user_id) where assigned_user_id is not null;

create table if not exists public.store_settings (
  id boolean primary key default true check (id),
  store_name text not null default 'THE GREEN SHOP',
  contact_phone text,
  contact_email text,
  operating_hours jsonb not null default '{}'::jsonb,
  delivery_enabled boolean not null default true,
  delivery_fee numeric(12,2) not null default 0 check (delivery_fee >= 0),
  minimum_order_value numeric(12,2) not null default 0 check (minimum_order_value >= 0),
  supported_payment_methods text[] not null default '{}',
  updated_by uuid references auth.users(id) on delete restrict,
  updated_at timestamptz not null default now()
);
insert into public.store_settings(id) values (true) on conflict (id) do nothing;

alter table public.categories enable row level security;
alter table public.products enable row level security;
alter table public.inventory enable row level security;
alter table public.inventory_movements enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;
alter table public.order_status_history enable row level security;
alter table public.payments enable row level security;
alter table public.payment_attempts enable row level security;
alter table public.payment_webhook_events enable row level security;
alter table public.payment_refunds enable row level security;
alter table public.cod_collections enable row level security;
alter table public.promotions enable row level security;
alter table public.promotion_products enable row level security;
alter table public.promotion_categories enable row level security;
alter table public.promotion_redemptions enable row level security;
alter table public.admin_notifications enable row level security;
alter table public.support_tickets enable row level security;
alter table public.store_settings enable row level security;

create policy "customers read own orders" on public.orders for select to authenticated using ((select auth.uid()) = customer_user_id);
create policy "customers read own order items" on public.order_items for select to authenticated using (exists (select 1 from public.orders o where o.id = order_id and o.customer_user_id = (select auth.uid())));
create policy "customers read own order history" on public.order_status_history for select to authenticated using (exists (select 1 from public.orders o where o.id = order_id and o.customer_user_id = (select auth.uid())));
create policy "customers read own payments" on public.payments for select to authenticated using (exists (select 1 from public.orders o where o.id = order_id and o.customer_user_id = (select auth.uid())));
create policy "customers read own payment attempts" on public.payment_attempts for select to authenticated using ((select auth.uid())=customer_user_id);
create policy "customers read own refunds" on public.payment_refunds for select to authenticated using (exists (select 1 from public.payments p join public.orders o on o.id=p.order_id where p.id=payment_id and o.customer_user_id=(select auth.uid())));
create policy "customers read own support" on public.support_tickets for select to authenticated using ((select auth.uid()) = customer_user_id);
create policy "catalog categories visible" on public.categories for select to authenticated using (is_visible);
create policy "catalog products visible" on public.products for select to authenticated using (is_visible and not is_archived);
create policy "catalog inventory visible" on public.inventory for select to authenticated using (true);
create policy "active promotions visible" on public.promotions for select to authenticated using (is_enabled and now() between starts_at and expires_at);
create policy "active promotion products visible" on public.promotion_products for select to authenticated using (exists (select 1 from public.promotions p where p.id=promotion_id and p.is_enabled and now() between p.starts_at and p.expires_at));
create policy "active promotion categories visible" on public.promotion_categories for select to authenticated using (exists (select 1 from public.promotions p where p.id=promotion_id and p.is_enabled and now() between p.starts_at and p.expires_at));
create policy "customers read own promotion redemptions" on public.promotion_redemptions for select to authenticated using ((select auth.uid())=customer_user_id);
create policy "store settings readable" on public.store_settings for select to authenticated using (true);

revoke all on public.inventory_movements, public.admin_notifications, public.payment_webhook_events, public.cod_collections from anon, authenticated;
grant select on public.categories, public.products, public.inventory, public.orders, public.order_items,
  public.order_status_history, public.payments, public.payment_attempts, public.payment_refunds, public.promotions, public.promotion_products,
  public.promotion_categories, public.promotion_redemptions, public.support_tickets, public.store_settings to authenticated;

