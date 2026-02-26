import { Controller, Get, Headers, Param } from '@nestjs/common';
import { VendorService } from './vendor.service';

@Controller('api/v1.0')
export class VendorController {
  constructor(private readonly vendorService: VendorService) {}

  @Get('vendors')
  async listVendors(@Headers('authorization') authHeader: string) {
    return this.vendorService.listVendors(authHeader);
  }

  @Get('vendors/:displayName')
  async getVendor(
    @Param('displayName') displayName: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.vendorService.getVendor(displayName, authHeader);
  }
}
