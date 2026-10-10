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
