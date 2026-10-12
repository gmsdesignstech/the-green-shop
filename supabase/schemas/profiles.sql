create schema if not exists private;

create table if not exists public.user_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  display_name text check (display_name is null or char_length(display_name) between 1 and 120),
  avatar_url text check (avatar_url is null or char_length(avatar_url) <= 2048),
  account_status text not null default 'active' check (account_status in ('active', 'suspended')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.user_profiles enable row level security;

create policy "users read own profile" on public.user_profiles
  for select to authenticated
  using ((select auth.uid()) = user_id);

create policy "users update own public profile fields" on public.user_profiles
  for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

revoke all on public.user_profiles from anon, authenticated;
grant select on public.user_profiles to authenticated;
grant update (display_name, avatar_url) on public.user_profiles to authenticated;

create or replace function private.sync_auth_user_profile()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.user_profiles (user_id, display_name, avatar_url)
  values (
    new.id,
    nullif(coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name'), ''),
    nullif(coalesce(new.raw_user_meta_data ->> 'avatar_url', new.raw_user_meta_data ->> 'picture'), '')
  )
  on conflict (user_id) do update
    set display_name = coalesce(public.user_profiles.display_name, excluded.display_name),
        avatar_url = coalesce(excluded.avatar_url, public.user_profiles.avatar_url),
        updated_at = now();
  return new;
end;
$$;

revoke all on function private.sync_auth_user_profile() from public, anon, authenticated;

drop trigger if exists on_auth_user_profile_sync on auth.users;
create trigger on_auth_user_profile_sync
  after insert or update of raw_user_meta_data on auth.users
  for each row execute function private.sync_auth_user_profile();

insert into public.user_profiles (user_id, display_name, avatar_url)
select
  id,
  nullif(coalesce(raw_user_meta_data ->> 'full_name', raw_user_meta_data ->> 'name'), ''),
  nullif(coalesce(raw_user_meta_data ->> 'avatar_url', raw_user_meta_data ->> 'picture'), '')
from auth.users
on conflict (user_id) do nothing;

create table if not exists public.customer_addresses (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  label text not null check (char_length(label) between 1 and 40),
  address_line text not null check (char_length(address_line) between 5 and 500),
  locality text not null check (char_length(locality) between 2 and 160),
  postal_code text check (postal_code is null or postal_code ~ '^[1-9][0-9]{5}$'),
  latitude numeric(9,6) check (latitude is null or latitude between -90 and 90),
  longitude numeric(9,6) check (longitude is null or longitude between -180 and 180),
  is_default boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check ((latitude is null) = (longitude is null))
);
create index if not exists customer_addresses_user_created_idx
  on public.customer_addresses(user_id, created_at desc);
create unique index if not exists customer_addresses_one_default_idx
  on public.customer_addresses(user_id) where is_default;

alter table public.customer_addresses enable row level security;
create policy "customers read own addresses" on public.customer_addresses for select to authenticated
  using ((select auth.uid()) = user_id);
create policy "customers create own addresses" on public.customer_addresses for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy "customers update own addresses" on public.customer_addresses for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
create policy "customers delete own addresses" on public.customer_addresses for delete to authenticated
  using ((select auth.uid()) = user_id);

revoke all on public.customer_addresses from anon, authenticated;
grant select,insert,update,delete on public.customer_addresses to authenticated;
grant usage,select on sequence public.customer_addresses_id_seq to authenticated;
