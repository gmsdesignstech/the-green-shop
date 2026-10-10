import { IsIn, IsString, Matches } from 'class-validator';

export class DevLoginDto {
  @Matches(/^\+91[6-9]\d{9}$/) phone!: string;
  @Matches(/^\d{6}$/) otp!: string;
  @IsIn(['customer', 'admin', 'delivery_partner']) role!: 'customer' | 'admin' | 'delivery_partner';
}

export class DevRefreshDto {
  @IsString() refreshToken!: string;
}

export class DevLogoutDto {
  @IsString() refreshToken!: string;
}
