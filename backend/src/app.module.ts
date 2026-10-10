import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { APP_GUARD } from '@nestjs/core';
import { ThrottlerGuard, ThrottlerModule } from '@nestjs/throttler';
import { AuthGuard } from './auth/auth.guard';
import { ApplicationsController } from './applications/applications.controller';
import { ApplicationsService } from './applications/applications.service';
import { DatabaseService } from './database.service';
import { AdminController } from './admin/admin.controller';
import { AdminGuard } from './admin/admin.guard';
import { AdminService } from './admin/admin.service';
import { RiderController } from './rider/rider.controller';
import { RiderGuard } from './rider/rider.guard';
import { RiderService } from './rider/rider.service';
import { TrackingController } from './tracking/tracking.controller';
import { TrackingService } from './tracking/tracking.service';
import { DevAuthController } from './dev-auth/dev-auth.controller';
import { DevAuthService } from './dev-auth/dev-auth.service';
import { PaymentsController } from './payments/payments.controller';
import { PaymentsService } from './payments/payments.service';
import { RazorpayProvider } from './payments/razorpay.provider';
import { HealthController } from './health/health.controller';

@Module({
  imports: [ConfigModule.forRoot({ isGlobal: true }), ThrottlerModule.forRoot([{ ttl: 60000, limit: 30 }])],
  controllers: [ApplicationsController, AdminController, RiderController, TrackingController, DevAuthController, PaymentsController, HealthController],
  providers: [
    DatabaseService,
    ApplicationsService,
    AuthGuard,
    AdminGuard,
    AdminService,
    RiderGuard,
    RiderService,
    TrackingService,
    DevAuthService,
    PaymentsService,
    RazorpayProvider,
    { provide: APP_GUARD, useClass: ThrottlerGuard },
  ],
})
export class AppModule {}
