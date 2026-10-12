import { Controller, Get, Query } from '@nestjs/common';
import { CatalogService } from './catalog.service';
import { CatalogQueryDto } from './dto';

@Controller('catalog')
export class CatalogController {
  constructor(private readonly service: CatalogService) {}

  @Get('products') products(@Query() query: CatalogQueryDto) { return this.service.products(query); }
  @Get('categories') categories() { return this.service.categories(); }
  @Get('promotions') promotions() { return this.service.promotions(); }
}
