import { Body, Controller, Get, Param, ParseIntPipe, Patch, Post, Query, Req, UseGuards } from '@nestjs/common';
import { AuthGuard, AuthenticatedRequest } from '../auth/auth.guard';
import { AdminGuard } from './admin.guard';
import { AdminService } from './admin.service';
import { ArchiveProductDto, AssignPartnerDto, CategoryDto, InventoryAdjustmentDto, OrderStatusDto, PageQueryDto, ProductDto, PromotionDto } from './dto';

@Controller('admin')
@UseGuards(AuthGuard, AdminGuard)
export class AdminController {
  constructor(private readonly service: AdminService) {}
  @Get('dashboard') dashboard(@Query('from') from?:string,@Query('to') to?:string){return this.service.dashboard(from,to);}
  @Get('products') products(@Query() q:PageQueryDto){return this.service.products(q);}
  @Post('products') createProduct(@Req() r:AuthenticatedRequest,@Body() d:ProductDto){return this.service.createProduct(r.user.id,d);}
  @Patch('products/:id') updateProduct(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number,@Body() d:ProductDto){return this.service.updateProduct(r.user.id,id,d);}
  @Patch('products/:id/archive') archive(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number,@Body() d:ArchiveProductDto){return this.service.archiveProduct(r.user.id,id,d.archived);}
  @Get('categories') categories(@Query() q:PageQueryDto){return this.service.categories(q);}
  @Post('categories') createCategory(@Req() r:AuthenticatedRequest,@Body() d:CategoryDto){return this.service.createCategory(r.user.id,d);}
  @Patch('categories/:id') updateCategory(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number,@Body() d:CategoryDto){return this.service.updateCategory(r.user.id,id,d);}
  @Get('promotions') promotions(@Query() q:PageQueryDto){return this.service.promotions(q);}
  @Post('promotions') createPromotion(@Req() r:AuthenticatedRequest,@Body() d:PromotionDto){return this.service.createPromotion(r.user.id,d);}
  @Patch('promotions/:id') updatePromotion(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number,@Body() d:PromotionDto){return this.service.updatePromotion(r.user.id,id,d);}
  @Get('inventory') inventory(@Query() q:PageQueryDto){return this.service.inventory(q);}
  @Post('inventory/:productId/adjustments') adjust(@Req() r:AuthenticatedRequest,@Param('productId',ParseIntPipe) id:number,@Body() d:InventoryAdjustmentDto){return this.service.adjustInventory(r.user.id,id,d);}
  @Get('orders') orders(@Query() q:PageQueryDto){return this.service.orders(q);}
  @Get('orders/:id') order(@Param('id',ParseIntPipe) id:number){return this.service.orderDetail(id);}
  @Patch('orders/:id/status') status(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number,@Body() d:OrderStatusDto){return this.service.changeOrderStatus(r.user.id,id,d);}
  @Post('orders/:id/assign-partner') assign(@Req() r:AuthenticatedRequest,@Param('id',ParseIntPipe) id:number,@Body() d:AssignPartnerDto){return this.service.assignPartner(r.user.id,id,d);}
  @Get('payments') payments(@Query() q:PageQueryDto){return this.service.payments(q);}
  @Get('reports') reports(@Query('from') from?:string,@Query('to') to?:string){return this.service.reports(from,to);}
  @Get('notifications') notifications(@Query() q:PageQueryDto){return this.service.notifications(q);}
  @Get('audit-logs') audit(@Query() q:PageQueryDto){return this.service.auditLogs(q);}
  @Get('support') support(@Query() q:PageQueryDto){return this.service.support(q);}
  @Get('store-settings') settings(){return this.service.storeSettings();}
}
