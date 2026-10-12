import { IsIn, IsString, Matches, MaxLength, MinLength } from 'class-validator';

export class DevLoginDto {
  @Matches(/^\+91[6-9]\d{9}$/) phone!: string;
  @Matches(/^\d{6}$/) otp!: string;
  @IsIn(['customer', 'admin', 'delivery_partner']) role!: 'customer' | 'admin' | 'delivery_partner';
}

export class DevRefreshDto {
  @IsString() @MinLength(32) @MaxLength(256) refreshToken!: string;
}

export class DevLogoutDto {
  @IsString() @MinLength(32) @MaxLength(256) refreshToken!: string;
}
