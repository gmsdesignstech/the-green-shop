import { Body, Controller, Post } from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { DevLoginDto, DevLogoutDto, DevRefreshDto } from './dto';
import { DevAuthService } from './dev-auth.service';

@Controller('dev-auth')
export class DevAuthController {
  constructor(private readonly auth:DevAuthService){}
  @Post('login') @Throttle({default:{limit:5,ttl:60_000}})
  login(@Body() dto:DevLoginDto){return this.auth.login(dto.phone,dto.otp,dto.role);}
  @Post('refresh') @Throttle({default:{limit:10,ttl:60_000}})
  refresh(@Body() dto:DevRefreshDto){return this.auth.refresh(dto.refreshToken);}
  @Post('logout') logout(@Body() dto:DevLogoutDto){return this.auth.logout(dto.refreshToken);}
}
