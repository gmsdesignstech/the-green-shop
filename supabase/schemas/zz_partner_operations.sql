create table if not exists public.delivery_partner_profiles (
  user_id uuid primary key references auth.users(id) on delete restrict,
  application_id bigint not null unique references public.delivery_partner_applications(id) on delete restrict,
  display_name text not null check (char_length(display_name) between 2 and 120),
  service_area text not null,
  vehicle_type text not null,
  profile_photo_path text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.partner_availability (
  partner_user_id uuid primary key references public.delivery_partner_profiles(user_id) on delete restrict,
  is_online boolean not null default false,
  version integer not null default 1 check (version > 0),
  updated_at timestamptz not null default now()
);

create table if not exists public.delivery_assignments (
  id bigint generated always as identity primary key,
  order_id bigint not null unique references public.orders(id) on delete restrict,
  partner_user_id uuid not null references public.delivery_partner_profiles(user_id) on delete restrict,
  status text not null default 'assigned' check (status in ('assigned','accepted','arrived_pickup','picked_up','out_for_delivery','delivered','failed','cancelled')),
  version integer not null default 1 check (version > 0),
  pickup_name text not null default 'THE GREEN SHOP',
  pickup_address jsonb not null default '{}'::jsonb,
  pickup_instructions text,
  delivery_instructions text,
  assigned_at timestamptz not null default now(),
  accepted_at timestamptz,
  picked_up_at timestamptz,
  completed_at timestamptz,
  failure_reason text,
  updated_at timestamptz not null default now()
);
create index if not exists delivery_assignments_partner_status_idx on public.delivery_assignments(partner_user_id,status,assigned_at desc);

create table if not exists public.delivery_assignment_history (
  id bigint generated always as identity primary key,
  assignment_id bigint not null references public.delivery_assignments(id) on delete restrict,
  from_status text,
  to_status text not null,
  actor_user_id uuid references auth.users(id) on delete restrict,
  reason text,
  created_at timestamptz not null default now()
);
create index if not exists delivery_assignment_history_assignment_idx on public.delivery_assignment_history(assignment_id,created_at);
create index if not exists delivery_assignment_history_actor_idx on public.delivery_assignment_history(actor_user_id) where actor_user_id is not null;

create table if not exists public.delivery_verifications (
  assignment_id bigint primary key references public.delivery_assignments(id) on delete restrict,
  otp_hash text not null,
  failed_attempts integer not null default 0 check (failed_attempts between 0 and 10),
  expires_at timestamptz not null,
  verified_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.partner_location_updates (
  id bigint generated always as identity primary key,
  assignment_id bigint not null references public.delivery_assignments(id) on delete restrict,
  partner_user_id uuid not null references public.delivery_partner_profiles(user_id) on delete restrict,
  latitude double precision not null check (latitude between -90 and 90),
  longitude double precision not null check (longitude between -180 and 180),
  accuracy_meters real check (accuracy_meters is null or accuracy_meters between 0 and 1000),
  heading_degrees real check (heading_degrees is null or heading_degrees between 0 and 360),
  speed_mps real check (speed_mps is null or speed_mps between 0 and 100),
  recorded_at timestamptz not null,
  received_at timestamptz not null default now(),
  unique (assignment_id, partner_user_id, recorded_at)
);
create index if not exists partner_locations_assignment_recorded_idx on public.partner_location_updates(assignment_id,recorded_at desc);
create index if not exists partner_locations_partner_recorded_idx on public.partner_location_updates(partner_user_id,recorded_at desc);
create index if not exists partner_locations_received_retention_idx on public.partner_location_updates(received_at);

create table if not exists public.partner_earnings (
  id bigint generated always as identity primary key,
  assignment_id bigint not null unique references public.delivery_assignments(id) on delete restrict,
  partner_user_id uuid not null references public.delivery_partner_profiles(user_id) on delete restrict,
  base_amount numeric(12,2) not null check (base_amount >= 0),
  incentive_amount numeric(12,2) not null default 0 check (incentive_amount >= 0),
  adjustment_amount numeric(12,2) not null default 0,
  status text not null default 'earned' check (status in ('estimated','earned','scheduled','paid','reversed')),
  earned_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists partner_earnings_partner_status_idx on public.partner_earnings(partner_user_id,status,created_at desc);

create table if not exists public.partner_payouts (
  id bigint generated always as identity primary key,
  partner_user_id uuid not null references public.delivery_partner_profiles(user_id) on delete restrict,
  amount numeric(12,2) not null check (amount > 0),
  status text not null check (status in ('pending','processing','paid','failed','cancelled')),
  provider_reference text unique,
  destination_mask text,
  initiated_at timestamptz not null default now(),
  completed_at timestamptz,
  updated_at timestamptz not null default now()
);
create index if not exists partner_payouts_partner_created_idx on public.partner_payouts(partner_user_id,initiated_at desc);

create table if not exists public.partner_notifications (
  id bigint generated always as identity primary key,
  partner_user_id uuid not null references public.delivery_partner_profiles(user_id) on delete cascade,
  type text not null,
  title text not null,
  message text not null,
  assignment_id bigint references public.delivery_assignments(id) on delete restrict,
  read_at timestamptz,
  created_at timestamptz not null default now()
);
create index if not exists partner_notifications_unread_idx on public.partner_notifications(partner_user_id,created_at desc) where read_at is null;

create table if not exists public.partner_support_tickets (
  id bigint generated always as identity primary key,
  partner_user_id uuid not null references public.delivery_partner_profiles(user_id) on delete restrict,
  assignment_id bigint references public.delivery_assignments(id) on delete restrict,
  category text not null check (category in ('customer_unavailable','incorrect_address','store_delay','missing_or_damaged','vehicle_breakdown','gps_navigation','payout','other')),
  description text not null check (char_length(description) between 5 and 2000),
  status text not null default 'open' check (status in ('open','in_progress','resolved','closed')),
  resolution text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists partner_support_partner_created_idx on public.partner_support_tickets(partner_user_id,created_at desc);
create index if not exists partner_support_assignment_idx on public.partner_support_tickets(assignment_id) where assignment_id is not null;

alter table public.delivery_partner_profiles enable row level security;
alter table public.partner_availability enable row level security;
alter table public.delivery_assignments enable row level security;
alter table public.delivery_assignment_history enable row level security;
alter table public.delivery_verifications enable row level security;
alter table public.partner_location_updates enable row level security;
alter table public.partner_earnings enable row level security;
alter table public.partner_payouts enable row level security;
alter table public.partner_notifications enable row level security;
alter table public.partner_support_tickets enable row level security;

create policy "partners read own profile" on public.delivery_partner_profiles for select to authenticated using ((select auth.uid())=user_id);
create policy "partners read own availability" on public.partner_availability for select to authenticated using ((select auth.uid())=partner_user_id);
create policy "partners read own assignments" on public.delivery_assignments for select to authenticated using ((select auth.uid())=partner_user_id);
create policy "partners read own assignment history" on public.delivery_assignment_history for select to authenticated using (exists(select 1 from public.delivery_assignments a where a.id=assignment_id and a.partner_user_id=(select auth.uid())));
create policy "partners read own earnings" on public.partner_earnings for select to authenticated using ((select auth.uid())=partner_user_id);
create policy "partners read own payouts" on public.partner_payouts for select to authenticated using ((select auth.uid())=partner_user_id);
create policy "partners read own notifications" on public.partner_notifications for select to authenticated using ((select auth.uid())=partner_user_id);
create policy "partners read own support" on public.partner_support_tickets for select to authenticated using ((select auth.uid())=partner_user_id);

revoke all on public.delivery_verifications,public.partner_location_updates from anon,authenticated;
grant select on public.delivery_partner_profiles,public.partner_availability,public.delivery_assignments,
  public.delivery_assignment_history,public.partner_earnings,public.partner_payouts,
  public.partner_notifications,public.partner_support_tickets to authenticated;
