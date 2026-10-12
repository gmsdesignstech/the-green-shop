import { Type } from 'class-transformer';
import { IsBoolean, IsInt, IsNumber, IsOptional, IsString, Length, Matches, Max, Min } from 'class-validator';

export class CustomerOrdersQueryDto {
  @IsOptional() @Type(() => Number) @IsInt() @Min(1) cursor?: number;
  @IsOptional() @Type(() => Number) @IsInt() @Min(1) @Max(50) limit = 20;
}

export class CustomerAddressDto {
  @IsString() @Length(1, 40) label!: string;
  @IsString() @Length(5, 500) addressLine!: string;
  @IsString() @Length(2, 160) locality!: string;
  @IsOptional() @IsString() @Matches(/^[1-9][0-9]{5}$/) postalCode?: string;
  @IsOptional() @Type(() => Number) @IsNumber() @Min(-90) @Max(90) latitude?: number;
  @IsOptional() @Type(() => Number) @IsNumber() @Min(-180) @Max(180) longitude?: number;
  @IsOptional() @IsBoolean() isDefault = false;
}
