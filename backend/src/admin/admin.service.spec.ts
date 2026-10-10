import { ConflictException, UnprocessableEntityException } from '@nestjs/common';
import { AdminService } from './admin.service';

describe('AdminService business rules', () => {
  it('rejects a compare-at price below the selling price before writing', async () => {
    const db = { transaction: jest.fn() } as any;
    const service = new AdminService(db);
    await expect(service.createProduct('admin', {
      name: 'Spinach', description: '', unit: '250 g', price: 50, compareAtPrice: 40, isVisible: true,
    })).rejects.toBeInstanceOf(UnprocessableEntityException);
    expect(db.transaction).not.toHaveBeenCalled();
  });

  it('prevents inventory adjustments that cannot be applied atomically', async () => {
    const client = { query: jest.fn().mockResolvedValue({ rowCount: 0, rows: [] }) };
    const db = { transaction: (work: any) => work(client) } as any;
    const service = new AdminService(db);
    await expect(service.adjustInventory('admin', 1, { delta: -99, reason: 'Damaged stock' }))
      .rejects.toBeInstanceOf(ConflictException);
    expect(client.query.mock.calls[0][0]).toContain('on_hand+$1 >= reserved');
  });

  it('rejects invalid order lifecycle transitions', async () => {
    const client = { query: jest.fn().mockResolvedValueOnce({ rowCount: 1, rows: [{ status: 'delivered', version: 3 }] }) };
    const db = { transaction: (work: any) => work(client) } as any;
    const service = new AdminService(db);
    await expect(service.changeOrderStatus('admin', 7, { status: 'cancelled', reason: 'late', version: 3 }))
      .rejects.toBeInstanceOf(UnprocessableEntityException);
  });

  it('rejects invalid promotion date ranges before writing', async () => {
    const db = { transaction: jest.fn() } as any;
    const service = new AdminService(db);
    await expect(service.createPromotion('admin', {
      title: 'Daily greens', description: '', kind: 'daily', discountType: 'percentage',
      discountValue: 10, minimumOrderValue: 0, startsAt: '2026-10-12T00:00:00Z',
      expiresAt: '2026-10-11T00:00:00Z', perCustomerLimit: 1, firstOrderOnly: false,
      terms: '', isEnabled: true,
    })).rejects.toBeInstanceOf(UnprocessableEntityException);
    expect(db.transaction).not.toHaveBeenCalled();
  });

  it('rejects percentage promotions over 100 percent', async () => {
    const db = { transaction: jest.fn() } as any;
    const service = new AdminService(db);
    await expect(service.createPromotion('admin', {
      title: 'Invalid offer', description: '', kind: 'festival', discountType: 'percentage',
      discountValue: 101, minimumOrderValue: 0, startsAt: '2026-10-11T00:00:00Z',
      expiresAt: '2026-10-12T00:00:00Z', perCustomerLimit: 1, firstOrderOnly: false,
      terms: '', isEnabled: true,
    })).rejects.toBeInstanceOf(UnprocessableEntityException);
  });
});
