create table if not exists public.trusted_role_assignments (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null check (role in ('customer', 'admin', 'delivery_partner')),
  status text not null default 'active' check (status in ('active', 'suspended')),
  granted_by uuid references auth.users(id) on delete restrict,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, role)
);

create table if not exists public.admin_access_requests (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  full_name text not null check (char_length(full_name) between 2 and 120),
  email text not null check (char_length(email) <= 320),
  phone text not null check (char_length(phone) between 8 and 20),
  business_name text not null check (char_length(business_name) between 2 and 160),
  reason text not null check (char_length(reason) between 10 and 2000),
  status text not null default 'pending' check (status in ('pending', 'under_review', 'approved', 'rejected', 'suspended')),
  decision_reason text check (decision_reason is null or char_length(decision_reason) <= 1000),
  reviewed_by uuid references auth.users(id) on delete restrict,
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists admin_access_requests_one_active_per_user
  on public.admin_access_requests(user_id)
  where status in ('pending', 'under_review', 'approved');
create index if not exists admin_access_requests_status_created_idx
  on public.admin_access_requests(status, created_at);
create index if not exists admin_access_requests_reviewed_by_idx
  on public.admin_access_requests(reviewed_by) where reviewed_by is not null;

create table if not exists public.delivery_partner_applications (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  full_name text not null check (char_length(full_name) between 2 and 120),
  phone text not null check (char_length(phone) between 8 and 20),
  email text check (email is null or char_length(email) <= 320),
  service_area text not null check (char_length(service_area) between 2 and 120),
  preferred_service_area text not null check (char_length(preferred_service_area) between 2 and 120),
  vehicle_type text not null check (vehicle_type in ('bicycle', 'motorcycle', 'scooter', 'car', 'van', 'on_foot')),
  driving_licence_number text check (driving_licence_number is null or char_length(driving_licence_number) <= 100),
  vehicle_registration_number text check (vehicle_registration_number is null or char_length(vehicle_registration_number) <= 100),
  consented_at timestamptz not null,
  status text not null default 'pending' check (status in ('draft', 'pending', 'under_review', 'approved', 'rejected', 'suspended')),
  decision_reason text check (decision_reason is null or char_length(decision_reason) <= 1000),
  reviewed_by uuid references auth.users(id) on delete restrict,
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists delivery_partner_applications_one_active_per_user
  on public.delivery_partner_applications(user_id)
  where status in ('draft', 'pending', 'under_review', 'approved');
create index if not exists delivery_partner_applications_status_created_idx
  on public.delivery_partner_applications(status, created_at);
create index if not exists delivery_partner_applications_reviewed_by_idx
  on public.delivery_partner_applications(reviewed_by) where reviewed_by is not null;

create table if not exists public.partner_document_metadata (
  id bigint generated always as identity primary key,
  application_id bigint not null references public.delivery_partner_applications(id) on delete cascade,
  owner_user_id uuid not null references auth.users(id) on delete cascade,
  document_type text not null check (document_type in ('identity', 'driving_licence', 'vehicle_registration', 'profile_photo')),
  storage_path text not null check (storage_path !~ '(^|/)\.\.(/|$)'),
  mime_type text not null check (mime_type in ('image/jpeg', 'image/png', 'application/pdf')),
  size_bytes bigint not null check (size_bytes > 0 and size_bytes <= 10485760),
  created_at timestamptz not null default now(),
  unique (application_id, document_type)
);
create index if not exists partner_document_metadata_owner_idx on public.partner_document_metadata(owner_user_id);

create table if not exists public.audit_logs (
  id bigint generated always as identity primary key,
  actor_user_id uuid references auth.users(id) on delete restrict,
  action text not null,
  target_type text not null,
  target_id text not null,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists private.dev_auth_sessions (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null check (role in ('admin', 'delivery_partner')),
  refresh_token_hash text not null unique,
  expires_at timestamptz not null,
  revoked_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists dev_auth_sessions_user_active_idx
  on private.dev_auth_sessions(user_id, expires_at desc) where revoked_at is null;
create index if not exists dev_auth_sessions_expiry_idx on private.dev_auth_sessions(expires_at);
revoke all on private.dev_auth_sessions from public, anon, authenticated;
create index if not exists audit_logs_actor_created_idx on public.audit_logs(actor_user_id, created_at desc);
create index if not exists audit_logs_target_idx on public.audit_logs(target_type, target_id, created_at desc);

alter table public.trusted_role_assignments enable row level security;
alter table public.admin_access_requests enable row level security;
alter table public.delivery_partner_applications enable row level security;
alter table public.partner_document_metadata enable row level security;
alter table public.audit_logs enable row level security;

create policy "applicants read own admin request" on public.admin_access_requests
  for select to authenticated using ((select auth.uid()) = user_id);
create policy "applicants read own partner application" on public.delivery_partner_applications
  for select to authenticated using ((select auth.uid()) = user_id);
create policy "applicants read own document metadata" on public.partner_document_metadata
  for select to authenticated using ((select auth.uid()) = owner_user_id);
create policy "applicants add own document metadata" on public.partner_document_metadata
  for insert to authenticated with check (
    (select auth.uid()) = owner_user_id and exists (
      select 1 from public.delivery_partner_applications a
      where a.id = application_id and a.user_id = (select auth.uid()) and a.status in ('draft', 'pending')
    )
  );

revoke all on public.trusted_role_assignments from anon, authenticated;
revoke all on public.audit_logs from anon, authenticated;
grant select on public.admin_access_requests, public.delivery_partner_applications to authenticated;
grant select, insert on public.partner_document_metadata to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('partner-documents', 'partner-documents', false, 10485760, array['image/jpeg','image/png','application/pdf'])
on conflict (id) do update set public=false, file_size_limit=excluded.file_size_limit, allowed_mime_types=excluded.allowed_mime_types;

create policy "partners upload own private documents" on storage.objects
  for insert to authenticated with check (
    bucket_id = 'partner-documents' and (storage.foldername(name))[1] = (select auth.uid())::text
  );
create policy "partners read own private documents" on storage.objects
  for select to authenticated using (
    bucket_id = 'partner-documents' and owner_id = (select auth.uid())::text
  );

