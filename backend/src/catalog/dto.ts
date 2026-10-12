import { Type } from 'class-transformer';
import { IsInt, IsOptional, IsString, Max, MaxLength, Min } from 'class-validator';

export class CatalogQueryDto {
  @IsOptional() @Type(() => Number) @IsInt() @Min(1) categoryId?: number;
  @IsOptional() @IsString() @MaxLength(160) search?: string;
  @IsOptional() @Type(() => Number) @IsInt() @Min(1) @Max(100) limit = 100;
}
