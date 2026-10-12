import { ServiceUnavailableException } from '@nestjs/common';
import { CatalogService } from './catalog.service';

describe('CatalogService', () => {
  const query = vi.fn();
  const service = new CatalogService({ query } as never);

  beforeEach(() => query.mockReset());

  it('returns only the database product rows and uses bound filters', async () => {
    query.mockResolvedValue({ rows: [{ id: '7', name: 'Tomato', available_quantity: 0 }] });
    await expect(service.products({ categoryId: 2, search: 'tom', limit: 20 })).resolves.toEqual({
      items: [{ id: '7', name: 'Tomato', available_quantity: 0 }],
    });
    expect(query.mock.calls[0][1]).toEqual([2, 'tom', 20]);
    expect(query.mock.calls[0][0]).toContain('p.is_visible=true');
    expect(query.mock.calls[0][0]).toContain('p.is_archived=false');
  });

  it('returns empty arrays without manufacturing fallback records', async () => {
    query.mockResolvedValue({ rows: [] });
    await expect(service.categories()).resolves.toEqual({ items: [] });
    await expect(service.promotions()).resolves.toEqual({ items: [] });
  });

  it('returns a safe 503 when the catalog database query fails', async () => {
    const failingService = new CatalogService({
      query: async () => { throw { code: 'ETIMEDOUT', message: 'connection timeout; password=secret' }; },
    } as never);

    let error: ServiceUnavailableException | undefined;
    try {
      await failingService.products({ limit: 20 });
    } catch (caught) {
      error = caught as ServiceUnavailableException;
    }
    expect(error).toBeDefined();
    expect(error).toBeInstanceOf(ServiceUnavailableException);
    expect(error!.getStatus()).toBe(503);
    expect(error!.getResponse()).toEqual({ status: 'error', code: 'CATALOG_TEMPORARILY_UNAVAILABLE' });
  });
});
