import { NotFoundException, UnprocessableEntityException } from '@nestjs/common';
import { CustomerService } from './customer.service';

describe('CustomerService', () => {
  it('scopes order history to the authenticated customer and paginates', async () => {
    const rows = [{ id: '9' }, { id: '8' }, { id: '7' }];
    const db = { query: vi.fn().mockResolvedValue({ rows }) } as any;
    const result = await new CustomerService(db).orders('customer-1', { limit: 2 });
    expect(result).toEqual({ items: rows.slice(0, 2), nextCursor: '8' });
    expect(db.query.mock.calls[0][0]).toContain('o.customer_user_id=$1');
    expect(db.query.mock.calls[0][1]).toEqual(['customer-1', Number.MAX_SAFE_INTEGER, 3]);
  });

  it('does not reveal an order owned by another customer', async () => {
    const db = { query: vi.fn().mockResolvedValue({ rowCount: 0, rows: [] }) } as any;
    await expect(new CustomerService(db).order('customer-1', 22)).rejects.toBeInstanceOf(NotFoundException);
    expect(db.query.mock.calls[0][1]).toEqual([22, 'customer-1']);
  });

  it('scopes address reads to the authenticated customer', async () => {
    const db = { query: vi.fn().mockResolvedValue({ rows: [] }) } as any;
    await expect(new CustomerService(db).addresses('customer-1')).resolves.toEqual({ items: [] });
    expect(db.query.mock.calls[0][1]).toEqual(['customer-1']);
    expect(db.query.mock.calls[0][0]).toContain('where user_id=$1');
  });

  it('rejects incomplete coordinate pairs before writing', async () => {
    const db = { transaction: vi.fn() } as any;
    expect(() => new CustomerService(db).createAddress('customer-1', {
      label: 'Home', addressLine: '1 Green Street', locality: 'Central', latitude: 12.9, isDefault: true,
    })).toThrow(UnprocessableEntityException);
    expect(db.transaction).not.toHaveBeenCalled();
  });

  it('uses ownership predicates when deleting addresses', async () => {
    const client = { query: vi.fn()
      .mockResolvedValueOnce({ rows: [] })
      .mockResolvedValueOnce({ rowCount: 0, rows: [] }) };
    const db = { transaction: (work: any) => work(client) } as any;
    await expect(new CustomerService(db).deleteAddress('customer-1', 4)).rejects.toBeInstanceOf(NotFoundException);
    expect(client.query.mock.calls[1][0]).toContain('id=$1 and user_id=$2');
    expect(client.query.mock.calls[1][1]).toEqual([4, 'customer-1']);
  });
});
