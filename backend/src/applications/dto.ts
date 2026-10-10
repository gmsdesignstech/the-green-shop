import { IsBoolean, IsEmail, IsIn, IsNotEmpty, IsOptional, IsPhoneNumber, IsString, Length, MaxLength } from 'class-validator';

export class AdminAccessRequestDto {
  @IsString() @Length(2, 120) fullName!: string;
  @IsEmail() @MaxLength(320) email!: string;
  @IsPhoneNumber() phone!: string;
  @IsString() @Length(2, 160) businessName!: string;
  @IsString() @Length(10, 2000) reason!: string;
}

export class PartnerApplicationDto {
  @IsString() @Length(2, 120) fullName!: string;
  @IsPhoneNumber() phone!: string;
  @IsOptional() @IsEmail() @MaxLength(320) email?: string;
  @IsString() @Length(2, 120) serviceArea!: string;
  @IsIn(['bicycle', 'motorcycle', 'scooter', 'car', 'van', 'on_foot']) vehicleType!: string;
  @IsOptional() @IsString() @MaxLength(100) drivingLicenceNumber?: string;
  @IsOptional() @IsString() @MaxLength(100) vehicleRegistrationNumber?: string;
  @IsBoolean() consent!: boolean;
}

export class ReviewApplicationDto {
  @IsIn(['under_review', 'approved', 'rejected', 'suspended']) status!: string;
  @IsOptional() @IsString() @MaxLength(1000) reason?: string;
}
