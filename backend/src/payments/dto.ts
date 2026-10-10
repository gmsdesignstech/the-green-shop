import { Type } from 'class-transformer';
import { ArrayMaxSize, ArrayMinSize, IsArray, IsIn, IsInt, IsOptional, IsString, Length, Matches, Min, ValidateNested } from 'class-validator';

export class CheckoutItemDto {
  @Type(() => Number) @IsInt() @Min(1) productId!: number;
  @Type(() => Number) @IsInt() @Min(1) quantity!: number;
}
export class CreateCheckoutDto {
  @IsArray() @ArrayMinSize(1) @ArrayMaxSize(100) @ValidateNested({each:true}) @Type(() => CheckoutItemDto) items!: CheckoutItemDto[];
  @IsIn(['razorpay','cod']) method!: 'razorpay'|'cod';
  @IsString() @Length(8,128) idempotencyKey!: string;
  @IsOptional() @IsString() @Length(3,40) couponCode?: string;
  @IsOptional() deliveryAddress?: Record<string,unknown>;
}
export class VerifyPaymentDto {
  @IsString() @Matches(/^pay_[A-Za-z0-9]+$/) paymentId!: string;
  @IsString() @Matches(/^order_[A-Za-z0-9]+$/) gatewayOrderId!: string;
  @IsString() @Matches(/^[a-f0-9]{64}$/i) signature!: string;
}
