import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request = require('supertest');
import { AppModule } from './app.module';
import { DatabaseService } from './database.service';

describe('public HTTP routes', () => {
  let app: INestApplication;

  const database = {
    query: vi.fn(async (sql: string) => {
      if (sql === 'select 1') return { rows: [{ '?column?': 1 }], rowCount: 1 };
      if (sql.includes('from public.categories')) return { rows: [], rowCount: 0 };
      if (sql.includes('from public.products')) return { rows: [], rowCount: 0 };
      if (sql.includes('from public.promotions')) return { rows: [], rowCount: 0 };
      throw new Error(`Unexpected test query: ${sql}`);
    }),
  };

  beforeAll(async () => {
    process.env.DEV_OTP_ENABLED = 'false';
    const module = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(DatabaseService)
      .useValue(database)
      .compile();
    app = module.createNestApplication();
    app.setGlobalPrefix('api/v1');
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, forbidNonWhitelisted: true, transform: true }));
    await app.init();
  });

  afterAll(() => app?.close());

  it.each([
    ['/api/v1/health', { status: 'ok', service: 'green-shop-api' }],
    ['/api/v1/health/database', { status: 'ok' }],
    ['/api/v1/catalog/categories', { items: [] }],
    ['/api/v1/catalog/products', { items: [] }],
    ['/api/v1/catalog/promotions', { items: [] }],
  ])('GET %s is registered', async (path, body) => {
    await request(app.getHttpServer()).get(path).expect(200).expect(body);
  });
});
