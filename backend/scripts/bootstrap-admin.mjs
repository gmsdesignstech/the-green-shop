import pg from 'pg';

const { Client } = pg;
const userId = process.env.BOOTSTRAP_ADMIN_USER_ID;
const confirmation = process.env.BOOTSTRAP_ADMIN_CONFIRM;
if (!process.env.DATABASE_URL || !userId || confirmation !== 'CREATE_FIRST_GREEN_SHOP_ADMIN') {
  throw new Error('DATABASE_URL, BOOTSTRAP_ADMIN_USER_ID and the explicit bootstrap confirmation are required');
}

const client = new Client({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.DATABASE_SSL === 'false' ? false : { rejectUnauthorized: true },
});

await client.connect();
try {
  await client.query('begin');
  const existing = await client.query(
    "select 1 from public.trusted_role_assignments where role='admin' and status='active' for update",
  );
  if (existing.rowCount) throw new Error('Bootstrap refused: an active administrator already exists');
  const user = await client.query('select id from auth.users where id=$1 and email_confirmed_at is not null', [userId]);
  if (!user.rowCount) throw new Error('Bootstrap refused: user is missing or email is unverified');
  await client.query(
    `insert into public.trusted_role_assignments(user_id, role, status)
     values($1, 'admin', 'active')
     on conflict(user_id, role) do update set status='active', updated_at=now()`,
    [userId],
  );
  await client.query(
    `insert into public.audit_logs(actor_user_id, action, target_type, target_id, metadata)
     values($1, 'admin.bootstrap', 'user', $1::text, '{"source":"controlled-script"}'::jsonb)`,
    [userId],
  );
  await client.query('commit');
  console.log('First administrator created successfully. Remove the bootstrap environment variables now.');
} catch (error) {
  await client.query('rollback');
  throw error;
} finally {
  await client.end();
}
