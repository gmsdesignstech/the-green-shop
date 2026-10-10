import { ApplicationsService } from './applications.service';

describe('ApplicationsService role access', () => {
  it('returns only database-authoritative access', async () => {
    const row = { admin: false, delivery_partner: false, admin_status: 'pending', partner_status: 'suspended' };
    const db = { query: jest.fn().mockResolvedValue({ rows: [row], rowCount: 1 }) } as any;

    await expect(new ApplicationsService(db).getMyAccess('user-1')).resolves.toEqual(row);
    expect(db.query).toHaveBeenCalledWith(expect.stringContaining('trusted_role_assignments'), ['user-1']);
  });

  it('reports approved active delivery access from the joined role and application state', async () => {
    const row = { admin: false, delivery_partner: true, admin_status: null, partner_status: 'approved' };
    const db = { query: jest.fn().mockResolvedValue({ rows: [row], rowCount: 1 }) } as any;

    await expect(new ApplicationsService(db).getMyAccess('partner-1')).resolves.toEqual(row);
    expect(db.query.mock.calls[0][0]).toContain("a.status='approved'");
  });
});
