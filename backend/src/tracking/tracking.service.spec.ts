import { NotFoundException } from '@nestjs/common';
import { TrackingService } from './tracking.service';

describe('TrackingService authorization', () => {
  it('scopes customer snapshots to the authenticated owner', async () => {
    const db = { query: jest.fn().mockResolvedValue({ rowCount: 1, rows: [{ order_id: 4 }] }) } as any;
    await new TrackingService(db).customerSnapshot('customer-1', 4);
    expect(db.query).toHaveBeenCalledWith(expect.stringContaining('o.customer_user_id=$2'), [4, 'customer-1']);
  });

  it('does not reveal another customer order', async () => {
    const db = { query: jest.fn().mockResolvedValue({ rowCount: 0, rows: [] }) } as any;
    await expect(new TrackingService(db).customerSnapshot('customer-2', 4)).rejects.toBeInstanceOf(NotFoundException);
  });

  it('returns only active assignments to administrators', async () => {
    const db = { query: jest.fn().mockResolvedValue({ rowCount: 0, rows: [] }) } as any;
    await new TrackingService(db).adminActiveDeliveries();
    expect(db.query.mock.calls[0][0]).toContain("a.status in ('assigned','accepted','arrived_pickup','picked_up','out_for_delivery')");
  });
});
