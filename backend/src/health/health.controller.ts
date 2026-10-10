import { Controller, Get, ServiceUnavailableException } from '@nestjs/common';
import { DatabaseService } from '../database.service';

@Controller('health')
export class HealthController {
  constructor(private readonly db:DatabaseService){}
  @Get() app(){return{status:'ok',service:'green-shop-api'};}
  @Get('database') async database(){try{await this.db.query('select 1');return{status:'ok'};}catch{throw new ServiceUnavailableException('Database health check failed');}}
  @Get('redis') redis(){return process.env.REDIS_URL?{status:'not_implemented',configured:true}:{status:'not_configured',configured:false};}
}
