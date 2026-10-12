import { Type } from 'class-transformer';
import { ArrayMaxSize, ArrayMinSize, IsArray, IsIn, IsInt, IsNumber, IsOptional, IsString, Length, Matches, Max, Min, ValidateNested } from 'class-validator';

export class CheckoutItemDto {
  @Type(() => Number) @IsInt() @Min(1) productId!: number;
  @Type(() => Number) @IsInt() @Min(1) quantity!: number;
}
export class CheckoutAddressDto {
  @IsString() @Length(1, 40) title!: string;
  @IsString() @Length(5, 500) addressLine!: string;
  @IsString() @Length(2, 160) locality!: string;
  @IsOptional() @Type(() => Number) @IsNumber() @Min(-90) @Max(90) latitude?: number;
  @IsOptional() @Type(() => Number) @IsNumber() @Min(-180) @Max(180) longitude?: number;
}
export class CreateCheckoutDto {
  @IsArray() @ArrayMinSize(1) @ArrayMaxSize(100) @ValidateNested({each:true}) @Type(() => CheckoutItemDto) items!: CheckoutItemDto[];
  @IsIn(['razorpay','cod']) method!: 'razorpay'|'cod';
  @IsString() @Length(8,128) idempotencyKey!: string;
  @IsOptional() @IsString() @Length(3,40) couponCode?: string;
  @ValidateNested() @Type(() => CheckoutAddressDto) deliveryAddress!: CheckoutAddressDto;
}
export class VerifyPaymentDto {
  @IsString() @Matches(/^pay_[A-Za-z0-9]+$/) paymentId!: string;
  @IsString() @Matches(/^order_[A-Za-z0-9]+$/) gatewayOrderId!: string;
  @IsString() @Matches(/^[a-f0-9]{64}$/i) signature!: string;
}
