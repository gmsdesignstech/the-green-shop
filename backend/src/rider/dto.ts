import { Type } from 'class-transformer';
import { ArrayMaxSize, IsArray, IsBoolean, IsDateString, IsIn, IsInt, IsNumber, IsOptional, IsString, Length, Max, MaxLength, Min, ValidateNested } from 'class-validator';

export class AvailabilityDto {
  @IsBoolean() isOnline!:boolean;
  @Type(()=>Number) @IsInt() @Min(1) version!:number;
}
export class DeliveryActionDto {
  @Type(()=>Number) @IsInt() @Min(1) version!:number;
  @IsOptional() @IsString() @MaxLength(1000) reason?:string;
  @IsOptional() @IsString() @Length(4,8) otp?:string;
  @IsOptional() @IsString() @MaxLength(100) idempotencyKey?:string;
}
export class LocationPointDto {
  @IsNumber() @Min(-90) @Max(90) latitude!:number;
  @IsNumber() @Min(-180) @Max(180) longitude!:number;
  @IsOptional() @IsNumber() @Min(0) @Max(1000) accuracyMeters?:number;
  @IsOptional() @IsNumber() @Min(0) @Max(360) headingDegrees?:number;
  @IsOptional() @IsNumber() @Min(0) @Max(100) speedMps?:number;
  @IsDateString() recordedAt!:string;
}
export class LocationBatchDto {
  @IsArray() @ArrayMaxSize(20) @ValidateNested({each:true}) @Type(()=>LocationPointDto) points!:LocationPointDto[];
}
export class RiderPageDto {
  @IsOptional() @IsString() cursor?:string;
  @IsOptional() @Type(()=>Number) @IsInt() @Min(1) @Max(100) limit=25;
  @IsOptional() @IsString() @MaxLength(40) status?:string;
}
export class SupportTicketDto {
  @IsOptional() @Type(()=>Number) @IsInt() @Min(1) assignmentId?:number;
  @IsIn(['customer_unavailable','incorrect_address','store_delay','missing_or_damaged','vehicle_breakdown','gps_navigation','payout','other']) category!:string;
  @IsString() @Length(5,2000) description!:string;
}
