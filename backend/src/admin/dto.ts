import { Type } from 'class-transformer';
import { IsBoolean, IsIn, IsInt, IsNumber, IsOptional, IsString, Length, Max, MaxLength, Min } from 'class-validator';

export class PageQueryDto {
  @IsOptional() @IsString() cursor?: string;
  @IsOptional() @Type(() => Number) @IsInt() @Min(1) @Max(100) limit = 25;
  @IsOptional() @IsString() @MaxLength(160) search?: string;
  @IsOptional() @IsString() @MaxLength(40) status?: string;
  @IsOptional() @IsString() @MaxLength(40) paymentStatus?: string;
  @IsOptional() @IsString() from?: string;
  @IsOptional() @IsString() to?: string;
}

export class ProductDto {
  @IsOptional() @Type(() => Number) @IsInt() @Min(1) categoryId?: number;
  @IsOptional() @IsString() @MaxLength(80) sku?: string;
  @IsString() @Length(2, 160) name!: string;
  @IsOptional() @IsString() @MaxLength(4000) description = '';
  @IsString() @Length(1, 40) unit!: string;
  @IsOptional() @IsString() @MaxLength(80) packageSize?: string;
  @Type(() => Number) @IsNumber({ maxDecimalPlaces: 2 }) @Min(0) price!: number;
  @IsOptional() @Type(() => Number) @IsNumber({ maxDecimalPlaces: 2 }) @Min(0) compareAtPrice?: number;
  @IsOptional() @IsString() imagePath?: string;
  @IsOptional() @IsBoolean() isVisible = true;
}

export class InventoryAdjustmentDto {
  @Type(() => Number) @IsInt() delta!: number;
  @IsString() @Length(3, 500) reason!: string;
  @IsOptional() @Type(() => Number) @IsInt() @Min(0) lowStockThreshold?: number;
}

export class OrderStatusDto {
  @IsIn(['confirmed','preparing','rider_assigned','out_for_delivery','delivered','cancelled']) status!: string;
  @IsOptional() @IsString() @MaxLength(500) reason?: string;
  @Type(() => Number) @IsInt() @Min(1) version!: number;
}

export class AssignPartnerDto {
  @IsString() partnerUserId!: string;
  @Type(() => Number) @IsInt() @Min(1) version!: number;
}

export class ArchiveProductDto { @IsBoolean() archived!: boolean; }

export class CustomerStatusDto {
  @IsIn(['active', 'suspended']) status!: 'active' | 'suspended';
  @IsString() @Length(3, 500) reason!: string;
}

export class CategoryDto {
  @IsString() @Length(2, 120) name!: string;
  @IsString() @Length(2, 140) slug!: string;
  @IsOptional() @IsString() @MaxLength(500) description?: string;
  @IsOptional() @IsString() @MaxLength(1000) imagePath?: string;
  @IsOptional() @Type(() => Number) @IsInt() sortOrder = 0;
  @IsOptional() @IsBoolean() isVisible = true;
}

export class PromotionDto {
  @IsString() @Length(2, 160) title!: string;
  @IsOptional() @IsString() @MaxLength(4000) description = '';
  @IsIn(['daily', 'festival', 'coupon']) kind!: string;
  @IsOptional() @IsString() @MaxLength(80) festivalName?: string;
  @IsOptional() @IsString() @Length(3, 40) code?: string;
  @IsIn(['fixed', 'percentage']) discountType!: string;
  @Type(() => Number) @IsNumber({ maxDecimalPlaces: 2 }) @Min(0.01) discountValue!: number;
  @IsOptional() @Type(() => Number) @IsNumber({ maxDecimalPlaces: 2 }) @Min(0) minimumOrderValue = 0;
  @IsOptional() @Type(() => Number) @IsNumber({ maxDecimalPlaces: 2 }) @Min(0) maximumDiscount?: number;
  @IsString() startsAt!: string;
  @IsString() expiresAt!: string;
  @IsOptional() @Type(() => Number) @IsInt() @Min(1) usageLimit?: number;
  @IsOptional() @Type(() => Number) @IsInt() @Min(1) perCustomerLimit = 1;
  @IsOptional() @IsBoolean() firstOrderOnly = false;
  @IsOptional() @IsString() @MaxLength(1000) bannerPath?: string;
  @IsOptional() @IsString() @MaxLength(4000) terms = '';
  @IsOptional() @IsBoolean() isEnabled = true;
}
