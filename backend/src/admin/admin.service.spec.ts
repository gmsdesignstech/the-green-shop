import { ConflictException, Logger, UnprocessableEntityException } from '@nestjs/common';
import { AdminService } from './admin.service';

describe('AdminService business rules', () => {
  it('uses schema-supported order payment states and computes the low-stock total', async () => {
    const db = {
      query: jest.fn()
        .mockResolvedValueOnce({ rows: [{ today_orders: 2, pending_orders: 1, completed_orders: 1, gross_sales: '125.00', payment_failures: 0, active_deliveries: 0 }] })
        .mockResolvedValueOnce({ rows: [] })
        .mockResolvedValueOnce({ rows: [] })
        .mockResolvedValueOnce({ rows: [] })
        .mockResolvedValueOnce({ rows: [{ id: 3, name: 'Spinach', total_count: 12 }] })
        .mockResolvedValueOnce({ rows: [] })
        .mockResolvedValueOnce({ rows: [{ count: 4 }] }),
    } as any;
    const service = new AdminService(db);

    const result = await service.dashboard();

    expect(db.query.mock.calls[0][0]).toContain("payment_status='captured'");
    expect(db.query.mock.calls[0][0]).toContain("'pending_payment'");
    expect(db.query.mock.calls[1][0]).toContain("payment_status='captured'");
    expect(result.metrics.low_stock_products).toBe(12);
    expect(result.metrics.pending_partner_applications).toBe(4);
    expect(result.unavailable).toEqual([]);
  });

  it('returns dashboard sections independently and logs full query failures', async () => {
    const error = new Error('self-signed certificate in certificate chain');
    const logError = jest.spyOn(Logger.prototype, 'error').mockImplementation();
    const db = { query: jest.fn().mockRejectedValue(error) } as any;
    const service = new AdminService(db);

    try {
      const result = await service.dashboard();

      expect(db.query).toHaveBeenCalledTimes(7);
      expect(result.unavailable).toEqual([
        'metrics',
        'sales',
        'recentOrders',
        'bestSellingProducts',
        'lowStock',
        'recentActivity',
        'pendingPartnerApplications',
      ]);
      expect(result.metrics.today_orders).toBeNull();
      expect(result.metrics.low_stock_products).toBeNull();
      expect(result.metrics.pending_partner_applications).toBeNull();
      expect(result.recentOrders).toEqual([]);
      expect(result.lowStock).toEqual([]);
      expect(logError).toHaveBeenCalledWith(
        'Admin dashboard query failed (metrics)',
        error.stack,
      );
    } finally {
      logError.mockRestore();
    }
  });

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
